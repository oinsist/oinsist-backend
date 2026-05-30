package com.oinsist.common.log.aspect;

import com.oinsist.common.core.service.CurrentUserProvider;
import com.oinsist.common.crypto.annotation.Sensitive;
import com.oinsist.common.crypto.support.SensitiveUtils;
import com.oinsist.common.log.annotation.OperLog;
import com.oinsist.common.log.event.OperLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截所有标注 {@code @OperLog} 的 Controller 方法，自动采集以下上下文信息：
 * <ul>
 *   <li>请求路径、HTTP 方法、请求参数（脱敏后）</li>
 *   <li>方法执行耗时</li>
 *   <li>当前操作人（通过 CurrentUserProvider 获取）</li>
 *   <li>异常信息（若方法执行失败）</li>
 * </ul>
 * <p>
 * 核心设计原则：
 * <ol>
 *   <li><b>不吞异常</b>：catch 后必须原样 throw，AOP 不能影响业务异常传播链路</li>
 *   <li><b>参数脱敏</b>：自动过滤 password、token、authorization 等敏感字段</li>
 *   <li><b>事件解耦</b>：采集完成后通过 Spring Event 发布，不直接操作数据库</li>
 *   <li><b>容错设计</b>：日志采集本身的异常不会影响主业务返回</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    /** 需要脱敏的参数名集合（全小写匹配） */
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "token", "authorization", "secret", "credentials"
    );

    private final ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private CurrentUserProvider currentUserProvider;

    public OperLogAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 环绕通知：拦截 @OperLog 注解标记的方法
     * <p>
     * 使用 @Around 而非 @AfterReturning + @AfterThrowing 的原因：
     * 需要精确计算方法执行耗时（startTime → endTime），
     * 且需要在 finally 块中统一处理成功/失败两种场景的日志发布。
     */
    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            error = e;
            throw e; // 必须原样抛出，不吞异常
        } finally {
            try {
                // 采集上下文并发布事件（在 finally 中确保无论成功/失败都记录）
                long duration = System.currentTimeMillis() - startTime;
                publishEvent(joinPoint, operLog, duration, error);
            } catch (Exception ex) {
                // 日志采集本身的异常不能影响主业务
                log.error("操作日志采集异常", ex);
            }
        }
    }

    /**
     * 采集请求上下文并发布操作日志事件
     */
    private void publishEvent(ProceedingJoinPoint joinPoint, OperLog operLog, long duration, Throwable error) {
        OperLogEvent event = new OperLogEvent();
        event.setTitle(operLog.title());
        event.setBusinessType(operLog.businessType().getCode());
        event.setDuration(duration);
        event.setOperTime(LocalDateTime.now());

        // 方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        event.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        // HTTP 请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            event.setRequestMethod(request.getMethod());
            event.setRequestUrl(request.getRequestURI());
            event.setIp(getClientIp(request));

            // 请求参数采集（需脱敏）
            if (operLog.isSaveRequestData()) {
                event.setRequestParam(sanitizeParams(joinPoint, signature));
            }
        }

        // 当前用户信息
        if (currentUserProvider != null) {
            Long userId = currentUserProvider.getUserId();
            event.setUserId(userId);
        }

        // 执行结果
        if (error != null) {
            event.setStatus(1); // 异常
            event.setErrorMsg(truncate(error.getMessage(), 2000));
        } else {
            event.setStatus(0); // 成功
        }

        eventPublisher.publishEvent(event);
    }

    /**
     * 参数脱敏：将方法参数转为安全的字符串表示
     * <p>
     * 脱敏策略：
     * <ol>
     *   <li>方法参数名本身命中敏感词 → 整个参数值替换为 "******"</li>
     *   <li>参数是对象类型 → 反射遍历其字段，命中敏感词的字段值替换为 "******"</li>
     *   <li>跳过 HttpServletRequest/HttpServletResponse 等不可序列化对象</li>
     * </ol>
     */
    private String sanitizeParams(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        if (paramNames == null || paramNames.length == 0) {
            return "";
        }

        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object value = paramValues[i];

            // 跳过 Servlet 相关不可序列化对象
            if (value instanceof jakarta.servlet.ServletRequest
                    || value instanceof jakarta.servlet.ServletResponse) {
                continue;
            }

            // 参数名本身是敏感字段
            if (SENSITIVE_PARAMS.contains(name.toLowerCase())) {
                paramMap.put(name, "******");
            } else if (value == null) {
                paramMap.put(name, null);
            } else if (isSimpleType(value.getClass())) {
                paramMap.put(name, value);
            } else if (value instanceof Collection<?> collection) {
                // 集合类型（如 List<Long>）：直接 toString() 安全输出，避免对 JDK 内部类反射
                paramMap.put(name, truncate(collection.toString(), 500));
            } else if (value instanceof Map<?, ?> map) {
                // Map 类型：直接 toString() 安全输出
                paramMap.put(name, truncate(map.toString(), 500));
            } else if (value.getClass().isArray()) {
                // 数组类型：安全转字符串（兼容基本类型数组与对象数组）
                paramMap.put(name, truncate(arrayToString(value), 500));
            } else {
                // 用户自定义 DTO：递归字段级脱敏
                paramMap.put(name, sanitizeObject(value));
            }
        }
        String result = paramMap.toString();
        return truncate(result, 2000);
    }

    /**
     * 对复杂对象进行字段级脱敏
     * <p>
     * 通过反射遍历对象的所有声明字段（包括父类），
     * 将敏感字段值替换为 "******"，其余字段保留原值的字符串表示。
     * <p>
     * 安全防护：对 JDK/Jakarta 标准库类型不做反射（Java 21 强模块系统会抛出
     * InaccessibleObjectException），直接返回 toString() 的安全表示。
     */
    private Map<String, Object> sanitizeObject(Object obj) {
        // 对 JDK/Jakarta 标准库类型不做反射，直接返回安全字符串
        String className = obj.getClass().getName();
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jakarta.")) {
            return Map.of("value", truncate(obj.toString(), 500));
        }

        Map<String, Object> fieldMap = new HashMap<>();
        Class<?> clazz = obj.getClass();

        // 遍历当前类及父类的字段
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                // 跳过静态字段和合成字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }

                String fieldName = field.getName();
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(obj);

                    // 【@Sensitive 注解级脱敏】优先检测字段上的 @Sensitive 注解，
                    // 与 Jackson SensitiveSerializer 共享同一套脱敏规则（SensitiveUtils），
                    // 确保日志输出与接口响应的脱敏结果一致
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (sensitive != null && fieldValue instanceof String strVal) {
                        fieldMap.put(fieldName, SensitiveUtils.desensitize(strVal, sensitive.type()));
                    } else if (SENSITIVE_PARAMS.contains(fieldName.toLowerCase())) {
                        // 参数名级别兜底：未标注 @Sensitive 但字段名命中敏感词，整体掩码
                        fieldMap.put(fieldName, "******");
                    } else {
                        fieldMap.put(fieldName, fieldValue);
                    }
                } catch (Exception e) {
                    // 反射访问失败（如 Java 模块系统限制），跳过该字段
                    fieldMap.put(fieldName, "[inaccessible]");
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fieldMap;
    }

    /**
     * 将数组安全转为字符串（兼容基本类型数组与对象数组）
     * <p>
     * Java 中 int[] 等基本类型数组不能强转为 Object[]，
     * 需要分别处理以避免 ClassCastException。
     */
    private String arrayToString(Object array) {
        if (array instanceof Object[] objArray) {
            return Arrays.deepToString(objArray);
        } else if (array instanceof int[] intArray) {
            return Arrays.toString(intArray);
        } else if (array instanceof long[] longArray) {
            return Arrays.toString(longArray);
        } else if (array instanceof double[] doubleArray) {
            return Arrays.toString(doubleArray);
        } else if (array instanceof float[] floatArray) {
            return Arrays.toString(floatArray);
        } else if (array instanceof boolean[] boolArray) {
            return Arrays.toString(boolArray);
        } else if (array instanceof byte[] byteArray) {
            return Arrays.toString(byteArray);
        } else if (array instanceof short[] shortArray) {
            return Arrays.toString(shortArray);
        } else if (array instanceof char[] charArray) {
            return Arrays.toString(charArray);
        }
        return array.toString();
    }

    /**
     * 判断是否为简单类型（基本类型、包装类型、String、枚举、时间类型等）
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.isEnum()
                || Number.class.isAssignableFrom(clazz)
                || CharSequence.class.isAssignableFrom(clazz)
                || Boolean.class == clazz
                || Character.class == clazz
                || java.time.temporal.Temporal.class.isAssignableFrom(clazz)
                || java.util.Date.class.isAssignableFrom(clazz);
    }

    /**
     * 获取客户端真实 IP
     * <p>
     * 优先从反向代理头 X-Forwarded-For 获取，其次取 X-Real-IP，
     * 最终降级到 getRemoteAddr()
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个（客户端真实 IP）
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 截断字符串，防止超长内容占满数据库字段
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
