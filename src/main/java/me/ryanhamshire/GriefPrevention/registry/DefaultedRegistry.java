package me.ryanhamshire.GriefPrevention.registry;

import java.util.Objects;
import java.util.function.Function;

public class DefaultedRegistry<T> extends Registry<T> {

    private boolean lightweightProvider;
    private Function<String, T> defaultProvider;

    public DefaultedRegistry(String name) {
        this(name, (T) null);
    }

    public DefaultedRegistry(String name, T defaultValue) {
        super(name);
        setDefault(defaultValue);
    }

    public DefaultedRegistry(String name, Function<String, T> defaultProvider) {
        super(name);
        setDefault(defaultProvider);
    }

    public Function<String, T> getDefault() {
        return defaultProvider;
    }

    public void setDefault(T defaultValue) {
        setDefault(k -> defaultValue);
        lightweightProvider = true;
    }

    public void setDefault(Function<String, T> defaultProvider) {
        Objects.requireNonNull(defaultProvider, "defaultProvider cannot be null");
        this.defaultProvider = defaultProvider;
        lightweightProvider = false;
    }

    @Override
    public T get(String key) {
        if (lightweightProvider) {
            return super.getOrDefault(key, getDefault().apply(key));
        } else {
            T t = super.get(key);
            return t == null ? getDefault().apply(key) : t;
        }
    }

}
