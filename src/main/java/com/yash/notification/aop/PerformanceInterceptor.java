package com.yash.notification.aop;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PerformanceInterceptor implements MethodInterceptor<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceInterceptor.class);

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        String methodName = context.getDeclaringType().getSimpleName() + "." + context.getMethodName();
        long start = System.currentTimeMillis();
        try {
            Object result = context.proceed();
            long duration = System.currentTimeMillis() - start;
            LOG.info("[PERF] {} executed in {} ms", methodName, duration);
            return result;
        } catch (Exception e) {
            LOG.error("[PERF] Exception in {}: {}", methodName, e.getMessage(), e);
            throw e;
        }
    }
}