/**
 * 操作日志公共模块
 * <p>
 * 提供 {@code @OperLog} 注解标记需要记录操作日志的方法，
 * 通过 AOP 切面自动采集请求上下文、执行耗时、异常信息，
 * 并以 Spring 事件机制发布，由业务模块监听落库。
 * <p>
 * 设计要点：
 * <ol>
 *   <li>本模块不依赖任何业务模块（oinsist-system），避免 common 反向依赖</li>
 *   <li>通过 Spring Event 解耦日志采集与日志持久化</li>
 *   <li>切面不吞异常，记录后原样抛出</li>
 *   <li>敏感参数（password/token/authorization）自动脱敏</li>
 * </ol>
 */
package com.oinsist.common.log;
