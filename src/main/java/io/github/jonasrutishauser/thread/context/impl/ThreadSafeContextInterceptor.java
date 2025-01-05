package io.github.jonasrutishauser.thread.context.impl;

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

import io.github.jonasrutishauser.thread.context.ThreadSafeScoped;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@ThreadSafeScopedInterceptor
@Priority(LIBRARY_BEFORE - 900)
public class ThreadSafeContextInterceptor {

    private final ThreadSafeContext context;
    private final Bean<?> target;

    @Inject
    ThreadSafeContextInterceptor(BeanManager beanManager, @Intercepted Bean<?> target) {
        this.context = (ThreadSafeContext) beanManager.getContext(ThreadSafeScoped.class);
        this.target = target;
    }

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        context.incrementUsage(target);
        try {
            return ctx.proceed();
        } finally {
            context.decrementUsage(target);
        }
    }
}
