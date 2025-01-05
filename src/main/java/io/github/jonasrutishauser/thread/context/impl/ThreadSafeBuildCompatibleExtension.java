package io.github.jonasrutishauser.thread.context.impl;

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

import io.github.jonasrutishauser.thread.context.ThreadSafeScoped;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.EventContext;

@SkipIfPortableExtensionPresent(ThreadSafeExtension.class)
public class ThreadSafeBuildCompatibleExtension implements BuildCompatibleExtension {

    @Priority(LIBRARY_BEFORE + 500)
    @Discovery
    public void addContext(MetaAnnotations metaAnnotations) {
        metaAnnotations.addContext(ThreadSafeScoped.class, true, ThreadSafeContext.class);
    }

    @Priority(LIBRARY_AFTER + 900)
    @Enhancement(types = Object.class, withSubtypes = true, withAnnotations = ThreadSafeScoped.class)
    public void addInterceptor(ClassConfig classConfig) {
        if (classConfig.info().hasAnnotation(ThreadSafeScoped.class)) {
            classConfig.addAnnotation(ThreadSafeScopedInterceptor.class);
        }
    }

    @Registration(types = Object.class)
    public void validateThreadSafeScopedBeans(BeanInfo bean, Messages messages) {
        if ("io.github.jonasrutishauser.thread.context.ThreadSafeScoped".equals(bean.scope().name())
                && (bean.isProducerField() || bean.isProducerMethod())) {
            messages.error("ThreadSafeScoped is not allowed on producer field or producer method in CDI Light", bean);
        }
    }

    @Priority(LIBRARY_BEFORE + 500)
    @Synthesis
    public void registerScopeShutdown(SyntheticComponents components, Types types) {
        components.addObserver(Shutdown.class) //
                .priority(LIBRARY_AFTER + 900) //
                .observeWith(ScopeShutdown.class);
    }

    public static class ScopeShutdown implements SyntheticObserver<Shutdown> {
        @Override
        public void observe(EventContext<Shutdown> event, Parameters params) throws Exception {
            ((ThreadSafeContext) CDI.current().getBeanManager().getContext(ThreadSafeScoped.class)).shutdown();
        }
    }

}
