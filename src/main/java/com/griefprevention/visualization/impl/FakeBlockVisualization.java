package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.BlockBoundaryVisualization;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.BoundaryVisualization;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A {@link BoundaryVisualization} implementation that displays clientside blocks along
 * {@link com.griefprevention.visualization.Boundary Boundaries}.
 */
public class FakeBlockVisualization extends BlockBoundaryVisualization
{

    protected final boolean waterTransparent;

    /**
     * Construct a new {@code FakeBlockVisualization}.
     *
     * @param world the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    public FakeBlockVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        super(world, visualizeFrom, height);

        // Water is considered transparent based on whether the visualization is initiated in water.
        waterTransparent = visualizeFrom.toBlock(world).getType() == Material.WATER;
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary)
    {
        return addBlockElement(switch (boundary.type())
        {
            case SUBDIVISION -> Material.IRON_BLOCK.createBlockData();
            case INITIALIZE_ZONE -> Material.DIAMOND_BLOCK.createBlockData();
            case CONFLICT_ZONE -> {
                BlockData fakeData = Material.REDSTONE_ORE.createBlockData();
                ((Lightable) fakeData).setLit(true);
                yield fakeData;
            }
            default -> Material.GLOWSTONE.createBlockData();
        });
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary)
    {
        // Determine BlockData from boundary type to cache for reuse in function.
        return addBlockElement(switch (boundary.type())
        {
            case ADMIN_CLAIM -> Material.PUMPKIN.createBlockData();
            case SUBDIVISION -> Material.WHITE_WOOL.createBlockData();
            case INITIALIZE_ZONE -> Material.DIAMOND_BLOCK.createBlockData();
            case CONFLICT_ZONE -> Material.NETHERRACK.createBlockData();
            default -> Material.GOLD_BLOCK.createBlockData();
        });
    }

    /**
     * Create a {@link Consumer} that adds an appropriate {@link FakeBlockElement} for the given {@link IntVector}.
     *
     * @param fakeData the fake {@link BlockData}
     * @return the function for determining a visible fake block location
     */
    protected @NotNull Consumer<@NotNull IntVector> addBlockElement(@NotNull BlockData fakeData)
    {
        return vector -> {
            // Create an element using our fake data and the determined block's real data.
            elements.add(new FakeBlockElement(vector, vector.toBlock(world).getBlockData(), fakeData));
        };
    }

    @Override
    public boolean isValidFloor(World world, int originalY, int x, int y, int z) {
        if (!isTransparent(world.getBlockAt(x, y, z))) {
            return isTransparent(world.getBlockAt(x, y + 1, z)) || isTransparent(world.getBlockAt(x, y - 1, z));
        }
        return false;
    }

    private boolean isTransparent(Block block) {
        Material blockMaterial = block.getType();

        // Custom per-material definitions.
        switch (blockMaterial)
        {
            case WATER:
                return waterTransparent;
            case SNOW:
                return false;
        }

        if (blockMaterial.isAir()
                || Tag.FENCES.isTagged(blockMaterial)
                || Tag.FENCE_GATES.isTagged(blockMaterial)
                || Tag.SIGNS.isTagged(blockMaterial)
                || Tag.WALLS.isTagged(blockMaterial)
                || Tag.WALL_SIGNS.isTagged(blockMaterial))
            return true;

        return blockMaterial.isTransparent();
    }

}
