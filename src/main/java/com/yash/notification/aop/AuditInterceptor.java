package com.yash.notification.aop;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

@Singleton
public class AuditInterceptor implements MethodInterceptor<Object, Object> {
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        String methodName = context.getDeclaringType().getSimpleName() + "." + context.getMethodName();
        Object[] args = context.getParameterValues();
        AUDIT_LOG.info("[AUDIT] Method: {} | Args: {} | Timestamp: {}", methodName, Arrays.toString(args),
                System.currentTimeMillis());
        return context.proceed();
    }
}