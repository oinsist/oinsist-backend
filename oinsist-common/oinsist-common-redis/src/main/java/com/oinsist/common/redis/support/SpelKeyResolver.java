package com.oinsist.common.redis.support;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * SpEL 表达式 Key 解析器
 * <p>
 * 将方法注解中的 SpEL 表达式解析为实际的 Redis key 值。
 * 通过 Spring 的 EvaluationContext 绑定方法参数，支持 #paramName 语法。
 * </p>
 */
@Component
public class SpelKeyResolver {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    
    /**
     * 解析 SpEL 表达式
     *
     * @param spel      SpEL 表达式字符串（如 "#orderId"）
     * @param joinPoint AOP 切点
     * @return 解析后的字符串值
     */
    public String resolve(String spel, JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, nameDiscoverer);
        
        Object value = parser.parseExpression(spel).getValue(context);
        return value == null ? "" : value.toString();
    }
}
