package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.BlockBoundaryVisualization;
import com.griefprevention.visualization.Boundary;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import me.ryanhamshire.GriefPrevention.util.ScoreboardColors;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * This visualization uses fake falling block entities that glow (can be seen through other blocks)<br>
 * It requires ProtocolLib to work.
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public class FakeFallingBlockVisualization extends BlockBoundaryVisualization {

    private static final HashMap<BlockData, Boolean> FLOOR_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    protected ArrayList<FakeFallingBlockElement> fallingElements = new ArrayList<>(64);

    /**
     * Construct a new {@code FallingBlockVisualization}.
     *
     * @param world         the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height        the height of the visualization
     */
    public FakeFallingBlockVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        super(world, visualizeFrom, height, 10, 128);
    }

    @Override
    protected void draw(@NotNull Player player, @NotNull Boundary boundary) {
        BoundingBox area = boundary.bounds();

        // Trim to area - allows for simplified display containment check later.
        BoundingBox displayZone = displayZoneArea.intersection(area);

        // If area is not inside display zone, there is nothing to display.
        if (displayZone == null) return;

        boolean is3d = boundary.claim() == null ? area.getMinY() >= worldMinHeight && area.getMaxY() <= worldMaxHeight : boundary.claim().is3D();
        Consumer<@NotNull IntVector> addCorner = addCornerElements(boundary, is3d);
        Consumer<@NotNull IntVector> addSide = addSideElements(boundary, is3d);

        // North and south boundaries
        for (int x = Math.max(area.getMinX() + step, displayZone.getMinX()); x < area.getMaxX() - step / 2 && x < displayZone.getMaxX(); x += step) {
            if (is3d) {
                addDisplayed(displayZone, new IntVector(x, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, area.getMaxY(), area.getMinZ()), addSide);

                addDisplayed(displayZone, new IntVector(x, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, area.getMinY(), area.getMinZ()), addSide);
            } else {
                addDisplayed(displayZone, new IntVector(x, findFloor(x, height, area.getMaxZ()), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, findFloor(x, height, area.getMinZ()), area.getMinZ()), addSide);
            }
        }

        // East and west boundaries
        for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step) {
            if (is3d) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), z), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), z), addSide);
            } else {
                addDisplayed(displayZone, new IntVector(area.getMinX(), findFloor(area.getMinX(), height, z), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), findFloor(area.getMaxX(), height, z), z), addSide);
            }
        }

        // resolve height for each corner so each corner is a consistent height
        int minMax = height;
        int maxMin = height;
        int maxMax = height;
        int minMin = height;
        if (!is3d) {
            minMax = findFloor(area.getMinX(), height, area.getMaxZ());
            maxMin = findFloor(area.getMaxX(), height, area.getMinZ());
            maxMax = findFloor(area.getMaxX(), height, area.getMaxZ());
            minMin = findFloor(area.getMinX(), height, area.getMinZ());
        }

        // First and last step are always directly adjacent to corners
        if (area.getLength() > 2) {
            if (is3d) {
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMinZ()), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMinY(), area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMinZ()), addSide);
            } else {
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, minMax, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, minMin, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, maxMax, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, maxMin, area.getMinZ()), addSide);
            }
        }

        if (area.getWidth() > 2) {
            if (is3d) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ() - 1), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ() - 1), addSide);
            } else {
                addDisplayed(displayZone, new IntVector(area.getMinX(), minMin, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMin, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), minMax, area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMax, area.getMaxZ() - 1), addSide);
            }
        }

        // up and down (if 3d)
        if (is3d) {
            for (int y = Math.max(area.getMinY() + step, displayZone.getMinY()); y < area.getMaxY() - step / 2 && y < displayZone.getMaxY(); y += step) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), y, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), y, area.getMinZ()), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), y, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), y, area.getMaxZ()), addSide);
            }
            if (area.getHeight() > 2) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY() - 1, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY() - 1, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY() - 1, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY() - 1, area.getMaxZ()), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY() + 1, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY() + 1, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY() + 1, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY() + 1, area.getMaxZ()), addSide);
            }
        }

        // Add corners last to override any other elements created by very small claims.
        if (is3d) {
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ()), addCorner);

            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ()), addCorner);
        } else {
            addDisplayed(displayZone, new IntVector(area.getMinX(), minMax, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMax, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), minMin, area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMin, area.getMinZ()), addCorner);
        }
    }

    @Override
    protected void apply(@NotNull Player player, @NotNull PlayerData playerData) {
        super.apply(player, playerData);
        // Apply all visualization elements.
        fallingElements.forEach(fe -> fe.draw(player, world));
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

    public @NotNull Consumer<@NotNull IntVector> addFallingBlockElement(@NotNull BlockData blockData, @NotNull Team teamColor) {
        return vector -> {
            fallingElements.add(new FakeFallingBlockElement(vector, teamColor, blockData));
        };
    }

    public int findFloor(int x, int y, int z) {
        return findFloor(world, x, y, z, 32, worldMinHeight, y - 32);
    }

    public FakeFallingBlockElement elementByEID(int entityId) {
        for (FakeFallingBlockElement element : fallingElements) {
            if (element.entityId() == entityId) return element;
        }
        return null;
    }

    public FakeFallingBlockElement elementByLocation(Location where) {
        if (getWorld() != where.getWorld()) return null;
        int x = where.getBlockX(), y = where.getBlockY(), z = where.getBlockZ();
        for (FakeFallingBlockElement element : fallingElements) {
            IntVector vec = element.getCoordinate();
            if (vec.x() == x && vec.y() == y && vec.z() == z) {
                return element;
            }
        }
        return null;
    }

    public World getWorld() {
        return this.world;
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
        FakeFallingBlockElement.eraseAll(player, fallingElements);
    }

    public static int findFloor(World world, int x, int y, int z, int depth, int minY, int def) {
        for (int i = y; i > y - depth && y > minY; i--) {
            Block block = world.getBlockAt(x, i, z);
            if (validFloor(block, block.getBlockData())) return i;
        }
        return def;
    }

    public static boolean validFloor(Block block, BlockData data) {
        Boolean isFullBlock = FLOOR_BLOCK_CACHE.get(data);
        if (isFullBlock == null) {
            isFullBlock = !block.isPassable()
                    && !Tag.LEAVES.isTagged(data.getMaterial());
            if (isFullBlock) {
                Collection<org.bukkit.util.BoundingBox> aabbs = block.getCollisionShape().getBoundingBoxes();
                if (!aabbs.isEmpty()) {
                    isFullBlock = aabbs.stream().mapToDouble(org.bukkit.util.BoundingBox::getVolume).sum() >= 0.8;
                }
                isFullBlock = isFullBlock && block.getBoundingBox().getVolume() >= 0.8;
            }
            // ensure that the same blockData is returned, otherwise this cache won't work and might memory leak
            if (block.getBlockData().hashCode() == block.getBlockData().hashCode()) FLOOR_BLOCK_CACHE.put(data, isFullBlock);
        }
        return isFullBlock;
    }

}
