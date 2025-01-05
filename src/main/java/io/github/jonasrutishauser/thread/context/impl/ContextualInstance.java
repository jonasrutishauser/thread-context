package io.github.jonasrutishauser.thread.context.impl;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

class ContextualInstance<T> {
    private final Contextual<T> contextual;
    private final CreationalContext<T> ctx;
    private T instance;

    public ContextualInstance(Contextual<T> contextual, CreationalContext<T> ctx) {
        this.contextual = contextual;
        this.ctx = ctx;
    }

    public T getInstance() {
        if (instance == null) {
            instance = contextual.create(ctx);
        }
        return instance;
    }

    public void destroy() {
        contextual.destroy(instance, ctx);
    }
}