package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.EntityBlockBoundaryVisualization;
import me.ryanhamshire.GriefPrevention.util.ScoreboardColors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * This visualization uses fake entities that glow (can be seen through other blocks)<br>
 * It requires ProtocolLib to work.
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public class FakeFallingBlockVisualization extends EntityBlockBoundaryVisualization<FakeFallingBlockElement> {

    private static final HashMap<BlockData, Boolean> FLOOR_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    /**
     * Construct a new {@code FakeFallingBlockVisualization}.
     *
     * @param world         the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height        the height of the visualization
     */
    public FakeFallingBlockVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        super(world, visualizeFrom, height);
    }

    public FakeFallingBlockVisualization(World world, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(world, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return switch (boundary.type()) {
            case ADMIN_CLAIM ->
                    addFallingElement(Material.ORANGE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.GOLD));
            case SUBDIVISION ->
                    addFallingElement(Material.WHITE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.WHITE));
            case INITIALIZE_ZONE, NATURE_RESTORATION_ZONE ->
                    addFallingElement(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.AQUA));
            case CONFLICT_ZONE ->
                    addFallingElement(Material.RED_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.RED));
            default ->
                    addFallingElement(Material.YELLOW_STAINED_GLASS.createBlockData(), ScoreboardColors.getTeamFor(ChatColor.YELLOW));
        };
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return addCornerElements(boundary);
    }

    protected @NotNull Consumer<@NotNull IntVector> addFallingElement(@NotNull BlockData blockData, @NotNull Team teamColor) {
        return vector -> {
            // don't draw over existing elements in the same position
            entityElements.putIfAbsent(vector, new FakeFallingBlockElement(vector, teamColor, blockData));
        };
    }

    @Override
    public void revert(Player player) {
        if (player != null) {
            FakeFallingBlockElement.eraseAllFallingBlocks(player, entityElements.values());
        }
    }

    @Override
    public boolean isValidFloor(World world, int originalY, int x, int y, int z) {
        return isFloor(world, originalY, x, y, z, 24);
    }

    public static boolean isFloor(World world, int originalY, int x, int y, int z, int ignoreSurroundsAfterY) {
        Block block = world.getBlockAt(x, y, z);
        return isFloorBlock(block) && (Math.abs(originalY - y) > ignoreSurroundsAfterY || (!isFloorBlock(block.getRelative(BlockFace.UP)) || !isFloorBlock(block.getRelative(BlockFace.DOWN))));
    }

    public static boolean isFloorBlock(Block block) {
        Boolean isFullBlock = FLOOR_BLOCK_CACHE.get(block.getBlockData());
        if (isFullBlock == null) {
            isFullBlock = !block.isPassable()
                    && !Tag.LEAVES.isTagged(block.getType());
            if (isFullBlock) {
                Collection<BoundingBox> aabbs = block.getCollisionShape().getBoundingBoxes();
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

    @Override
    public boolean isValidFloor(Block block) {
        throw new UnsupportedOperationException("not implemented. use isValidFloor(org.bukkit.World, int, int, int, int)");
    }

}
