package io.github.jonasrutishauser.thread.context.impl;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InterceptorBinding;

@Target(TYPE)
@Retention(RUNTIME)
@InterceptorBinding
@interface ThreadSafeScopedInterceptor {
    class Literal extends AnnotationLiteral<ThreadSafeScopedInterceptor> implements ThreadSafeScopedInterceptor {
        private static final long serialVersionUID = 1L;

        public static final ThreadSafeScopedInterceptor INSTANCE = new Literal();

        private Literal() {
        }
    }
}
