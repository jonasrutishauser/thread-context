package io.github.jonasrutishauser.thread.context.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.jonasrutishauser.thread.context.ThreadSafeScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

public class ThreadSafeContext implements AlterableContext {

    private final ConcurrentMap<Contextual<?>, Queue<ContextualInstance<?>>> beans = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<Contextual<?>, ContextualInstance<?>>> boundBeans = ThreadLocal.withInitial(HashMap::new);
    private final ConcurrentMap<ContextualInstance<?>, Integer> beanUsage = new ConcurrentHashMap<>();

    private final AtomicBoolean active = new AtomicBoolean(true);

    void incrementUsage(Contextual<?> contextual) {
        beanUsage.compute(boundBeans.get().get(contextual), (key, value) -> Integer.valueOf(value.intValue() + 1));
    }

    void decrementUsage(Contextual<?> contextual) {
        Map<Contextual<?>, ContextualInstance<?>> instances = boundBeans.get();
        Integer usage = beanUsage.compute(instances.get(contextual), (key, value) -> Integer.valueOf(value.intValue() - 1));
        if (usage.intValue() <= 0) {
            ContextualInstance<?> contextualInstance = instances.remove(contextual);
            beanUsage.remove(contextualInstance);
            beans.computeIfAbsent(contextual, key -> new ConcurrentLinkedQueue<>()).add(contextualInstance);
            if (instances.isEmpty()) {
                boundBeans.remove();
            }
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ThreadSafeScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (!isActive()) {
            throw new ContextNotActiveException();
        }
        Map<Contextual<?>, ContextualInstance<?>> instances = boundBeans.get();
        @SuppressWarnings("unchecked")
        ContextualInstance<T> contextualInstance = (ContextualInstance<T>) instances.computeIfAbsent(contextual, key -> {
            Queue<ContextualInstance<?>> queue = beans.get(key);
            ContextualInstance<?> instance = queue == null ? null : queue.poll();
            return instance == null ? new ContextualInstance<>(contextual, creationalContext) : instance;
        });
        beanUsage.putIfAbsent(contextualInstance, Integer.valueOf(0));
        return contextualInstance.getInstance();
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        if (!isActive()) {
            throw new ContextNotActiveException();
        }
        Map<Contextual<?>, ContextualInstance<?>> instances = boundBeans.get();
        if (instances.isEmpty()) {
            boundBeans.remove();
            return null;
        }
        @SuppressWarnings("unchecked")
        ContextualInstance<T> contextualInstance = (ContextualInstance<T>) instances.get(contextual);
        return contextualInstance == null ? null : contextualInstance.getInstance();
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        ContextualInstance<?> instance;
        while ((instance = beans.get(contextual).poll()) != null) {
            instance.destroy();
        }
        beans.computeIfPresent(contextual, (key, queue) -> queue.isEmpty() ? null : queue);
    }

    void shutdown() {
        if (active.compareAndSet(true, false)) {
            beans.forEach((key, queue) -> queue.forEach(ContextualInstance::destroy));
            beans.clear();
            // there shouldn't be any boundBeans therefore the next shouldn't do anything
            beanUsage.keySet().forEach(ContextualInstance::destroy);
            beanUsage.clear();
        }
    }

}
