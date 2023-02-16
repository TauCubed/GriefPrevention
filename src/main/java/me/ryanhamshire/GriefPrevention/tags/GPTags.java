package me.ryanhamshire.GriefPrevention.tags;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.Arrays;

public class GPTags {

    public static final Tag<Material> SPAWN_EGGS = new HashSetTag<>(new NamespacedKey(GriefPrevention.instance, "spawn_eggs"),
            Arrays.stream(Material.values()).filter(m -> m.toString().endsWith("_SPAWN_EGG")).toList());

    public static final Tag<Material> DYES = new HashSetTag<>(new NamespacedKey(GriefPrevention.instance, "spawn_eggs"),
            Arrays.stream(Material.values()).filter(m -> m.toString().endsWith("_DYE")).toList());

}
