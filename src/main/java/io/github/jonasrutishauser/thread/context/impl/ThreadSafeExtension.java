package io.github.jonasrutishauser.thread.context.impl;

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.github.jonasrutishauser.thread.context.ThreadSafeScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessProducer;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.WithAnnotations;

public class ThreadSafeExtension implements Extension {

    private final ThreadSafeContext context = new ThreadSafeContext();

    void addInterceptor(@Observes @WithAnnotations(ThreadSafeScoped.class) ProcessAnnotatedType<?> event) {
        if (event.getAnnotatedType().isAnnotationPresent(ThreadSafeScoped.class)) {
            event.configureAnnotatedType().add(ThreadSafeScopedInterceptor.Literal.INSTANCE);
        }
    }

    <T> void addInterceptor(@Observes ProcessProducer<?, T> event, BeanManager beanManager) {
        if (event.getAnnotatedMember().isAnnotationPresent(ThreadSafeScoped.class)) {
            Type baseType = event.getAnnotatedMember().getBaseType();
            Type producedClass = baseType instanceof ParameterizedType type ? type.getRawType() : baseType;
            Producer<T> producer = event.getProducer();
            event.configureProducer().produceWith(ctx -> {
                T bean = producer.produce(ctx);
                try {
                    @SuppressWarnings("unchecked")
                    InterceptionFactory<T> factory = createThreadSafeScopedInterceptionFactory(beanManager, ctx,
                            (Class<T>) producedClass);
                    return factory.createInterceptedInstance(bean);
                } catch (UnproxyableResolutionException | ClassCastException e) {
                    @SuppressWarnings("unchecked")
                    InterceptionFactory<T> factory = createThreadSafeScopedInterceptionFactory(beanManager, ctx,
                            (Class<T>) bean.getClass());
                    return factory.createInterceptedInstance(bean);
                }
            });
        }
    }

    private <T> InterceptionFactory<T> createThreadSafeScopedInterceptionFactory(BeanManager beanManager,
            CreationalContext<T> ctx, Class<T> type) {
        InterceptionFactory<T> factory = beanManager.createInterceptionFactory(ctx, type);
        factory.configure().add(ThreadSafeScopedInterceptor.Literal.INSTANCE);
        return factory;
    }

    void registerContext(@Observes AfterBeanDiscovery event) {
        event.addContext(context);
        try {
            Class<?> shutdownEvent = Class.forName("jakarta.enterprise.event.Shutdown", false,
                    getClass().getClassLoader());
            event.addObserverMethod() //
                    .observedType(shutdownEvent) //
                    .priority(LIBRARY_AFTER + 500) //
                    .notifyWith(e -> context.shutdown());
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    void shutdown(@Observes BeforeShutdown event) {
        context.shutdown();
    }

}
