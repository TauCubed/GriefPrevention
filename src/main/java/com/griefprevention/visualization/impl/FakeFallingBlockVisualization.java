package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.BlockBoundaryVisualization;
import com.griefprevention.visualization.Boundary;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.util.ScoreboardColors;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * This visualization uses fake falling block entities that glow (can be seen through other blocks)<br>
 * It requires ProtocolLib to work.
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public class FakeFallingBlockVisualization extends BlockBoundaryVisualization {

    private static final HashMap<BlockData, Boolean> FLOOR_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    protected HashMap<IntVector, FakeFallingBlockElement> fallingElements = new HashMap<>(32);

    protected int lastHeight = height;

    /**
     * Construct a new {@code FallingBlockVisualization}.
     *
     * @param world         the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height        the height of the visualization
     */
    public FakeFallingBlockVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        this(world, visualizeFrom, height, 10, 128);
    }

    public FakeFallingBlockVisualization(World world, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(world, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected void apply(@NotNull Player player, @NotNull PlayerData playerData) {
        super.apply(player, playerData);
        // Apply all visualization elements.
        for (FakeFallingBlockElement element : fallingElements.values()) element.draw(player, world);
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return switch (boundary.type()) {
            case ADMIN_CLAIM ->
                    addFallingBlockElement(Material.ORANGE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.GOLD));
            case SUBDIVISION ->
                    addFallingBlockElement(Material.WHITE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.WHITE));
            case INITIALIZE_ZONE, NATURE_RESTORATION_ZONE ->
                    addFallingBlockElement(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.AQUA));
            case CONFLICT_ZONE ->
                    addFallingBlockElement(Material.RED_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.RED));
            default ->
                    addFallingBlockElement(Material.YELLOW_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.YELLOW));
        };
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return addCornerElements(boundary);
    }

    protected @NotNull Consumer<@NotNull IntVector> addFallingBlockElement(@NotNull BlockData blockData, @NotNull Team teamColor) {
        return vector -> {
            // don't draw over existing elements
            fallingElements.putIfAbsent(vector, new FakeFallingBlockElement(vector, teamColor, blockData));
        };
    }

    public FakeFallingBlockElement elementByEID(int entityId) {
        for (FakeFallingBlockElement element : fallingElements.values()) {
            if (element.entityId() == entityId) return element;
        }
        return null;
    }

    public FakeFallingBlockElement elementByLocation(Location where) {
        if (getWorld() != where.getWorld()) return null;
        return fallingElements.get(new IntVector(where));
    }

    public World getWorld() {
        return this.world;
    }

    public Collection<FakeFallingBlockElement> getElements() {
        return Collections.unmodifiableCollection(fallingElements.values());
    }

    @Override
    public void revert(@Nullable Player player) {
        if (canVisualize(player)) erase(player);
    }

    @Override
    protected void erase(@NotNull Player player, @NotNull Boundary boundary) {
        erase(player);
    }

    protected void erase(@NotNull Player player) {
        FakeFallingBlockElement.eraseAll(player, fallingElements.values());
    }

    @Override
    public boolean isValidFloor(Block block) {
        Boolean isFullBlock = FLOOR_BLOCK_CACHE.get(block.getBlockData());
        if (isFullBlock == null) {
            isFullBlock = !block.isPassable()
                    && !Tag.LEAVES.isTagged(block.getType());
            if (isFullBlock) {
                Collection<org.bukkit.util.BoundingBox> aabbs = block.getCollisionShape().getBoundingBoxes();
                if (!aabbs.isEmpty()) {
                    isFullBlock = aabbs.stream().mapToDouble(org.bukkit.util.BoundingBox::getVolume).sum() >= 0.8;
                }
                isFullBlock = isFullBlock && block.getBoundingBox().getVolume() >= 0.8;
            }
            // ensure that the same blockData is returned, otherwise this cache won't work and might memory leak
            if (block.getBlockData().hashCode() == block.getBlockData().hashCode()) FLOOR_BLOCK_CACHE.put(block.getBlockData(), isFullBlock);
        }
        return isFullBlock;
    }

}
