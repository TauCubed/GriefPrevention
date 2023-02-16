package me.ryanhamshire.GriefPrevention.tags;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HashSetTag<T extends Keyed> implements Tag<T> {

    private final NamespacedKey key;

    private final HashSet<T> tagged = new HashSet<>();
    private final Set<T> unmodifiableTagged = Collections.unmodifiableSet(tagged);

    public HashSetTag(NamespacedKey key) {
        Objects.requireNonNull(key);
        this.key = key;
    }

    public HashSetTag(NamespacedKey key, Collection<T> tags) {
        this(key);
        tagged.addAll(tags);
    }

    public boolean add(@NotNull T item) {
        Objects.requireNonNull(item);
        return tagged.add(item);
    }

    public boolean addAll(@NotNull Collection<T> tags) {
        return tagged.addAll(tags);
    }

    public boolean remove(T item) {
        return tagged.remove(item);
    }

    @Override
    public boolean isTagged(@NotNull T item) {
        return tagged.contains(item);
    }

    @NotNull
    @Override
    public Set<T> getValues() {
        return unmodifiableTagged;
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return key;
    }

}
