package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public abstract class EntityBlockBoundaryVisualization<T extends FakeEntityElement> extends BlockBoundaryVisualization {

    protected HashMap<IntVector, T> entityElements = new HashMap<>(32);

    public EntityBlockBoundaryVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        this(world, visualizeFrom, height, 10, 128);
    }

    public EntityBlockBoundaryVisualization(World world, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(world, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected void apply(@NotNull Player player, @NotNull PlayerData playerData) {
        super.apply(player, playerData);
        // Apply all visualization elements.
        for (T element : entityElements.values()) element.draw(player, world);
    }

    public T elementByEID(int entityId) {
        for (T element : entityElements.values()) {
            if (element.entityId() == entityId) return element;
        }
        return null;
    }

    public T elementByLocation(Location where) {
        if (getWorld() != where.getWorld()) return null;
        return entityElements.get(new IntVector(where));
    }

    public World getWorld() {
        return this.world;
    }

    public Collection<T> getElements() {
        return Collections.unmodifiableCollection(entityElements.values());
    }

    @Override
    protected void erase(@NotNull Player player, @NotNull Boundary boundary) {
        revert(player);
    }

}
