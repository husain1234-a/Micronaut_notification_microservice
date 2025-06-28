package com.yash.notification.aop;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

@Singleton
public class LoggingInterceptor implements MethodInterceptor<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        String methodName = context.getDeclaringType().getSimpleName() + "." + context.getMethodName();
        Object[] args = context.getParameterValues();
        LOG.info("[AOP] Entering: {} with args: {}", methodName, Arrays.toString(args));
        long start = System.currentTimeMillis();
        try {
            Object result = context.proceed();
            LOG.info("[AOP] Exiting: {} with result: {} ({} ms)", methodName, result,
                    (System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            LOG.error("[AOP] Exception in {}: {}", methodName, e.getMessage(), e);
            throw e;
        }
    }
}