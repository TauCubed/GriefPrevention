package me.ryanhamshire.GriefPrevention.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class Registry<T> {

    private final String name;
    private final HashMap<String, T> map = new HashMap<>();

    public Registry(String name) {
        Objects.requireNonNull(name, "Registry name cannot be null");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isRegistered(String key) {
        return map.containsKey(key);
    }

    public T get(String key) {
        return map.get(key);
    }

    public T getOrDefault(String key, T def) {
        return map.getOrDefault(key, def);
    }

    public T register(String key, T value) {
        return map.put(key, value);
    }

    public T unregister(String key) {
        return map.remove(key);
    }

    public Collection<T> items() {
        return map.values();
    }

    public Set<String> keys() {
        return map.keySet();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public HashMap<String, T> mapView() {
        return map;
    }

}
