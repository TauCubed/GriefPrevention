package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public abstract class BlockBoundaryVisualization extends BoundaryVisualization
{

    protected final int step;
    protected final BoundingBox displayZoneArea;
    protected final Collection<BlockElement> elements = new ArrayList<>();

    protected final int worldMaxHeight = world.getMaxHeight();
    protected final int worldMinHeight = world.getMinHeight();

    /**
     * Construct a new {@code BlockBoundaryVisualization} with a step size of {@code 10} and a display radius of
     * {@code 75}.
     *
     * @param world the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    protected BlockBoundaryVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height)
    {
        this(world, visualizeFrom, height, 10, 75);
    }

    /**
     * Construct a new {@code BlockBoundaryVisualization}.
     *
     * @param world the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     * @param step the distance between individual side elements
     * @param displayZoneRadius the radius in which elements are visible from the visualization location
     */
    protected BlockBoundaryVisualization(
            @NotNull World world,
            @NotNull IntVector visualizeFrom,
            int height,
            int step,
            int displayZoneRadius)
    {
        super(world, visualizeFrom, height);
        this.step = step;
        this.displayZoneArea = new BoundingBox(
                visualizeFrom.add(-displayZoneRadius, -displayZoneRadius, -displayZoneRadius),
                visualizeFrom.add(displayZoneRadius, displayZoneRadius, displayZoneRadius));
    }

    @Override
    protected void apply(@NotNull Player player, @NotNull PlayerData playerData) {
        super.apply(player, playerData);
        elements.forEach(element -> element.draw(player, world));
    }

    @Override
    protected void draw(@NotNull Player player, @NotNull Boundary boundary)
    {
        BoundingBox area = boundary.bounds();

        // Trim to area - allows for simplified display containment check later.
        BoundingBox displayZone = displayZoneArea.intersection(area);

        // If area is not inside display zone, there is nothing to display.
        if (displayZone == null) return;

        boolean is3d = area.getMaxY() < Claim._2D_HEIGHT;
        Consumer<@NotNull IntVector> addCorner = addCornerElements(boundary);
        Consumer<@NotNull IntVector> addSide = addSideElements(boundary);

        // we render a cube for 3d boundaries, otherwise we render a square on the "floor" for 2d boundaries
        if (is3d) {
            // Add corners first to override any other elements created by very small claims.
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ()), addCorner);

            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ()), addCorner);

            // North and south boundaries
            for (int x = Math.max(area.getMinX() + step, displayZone.getMinX()); x < area.getMaxX() - step / 2 && x < displayZone.getMaxX(); x += step) {
                addDisplayed(displayZone, new IntVector(x, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, area.getMaxY(), area.getMinZ()), addSide);

                addDisplayed(displayZone, new IntVector(x, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, area.getMinY(), area.getMinZ()), addSide);
            }

            // East and west boundaries
            for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), z), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), z), addSide);
            }

            // First and last step are always directly adjacent to corners
            if (area.getLength() > 2) {
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMinZ()), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, area.getMinY(), area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMinZ()), addSide);
            }

            if (area.getWidth() > 2) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ() - 1), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ() - 1), addSide);
            }

            // extra logic for the vertical direction
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
        } else { // if this boundary is 2d
            // resolve height for each corner so each corner is a consistent height
            int minMax = findFloor(area.getMinX(), height, area.getMaxZ());
            int maxMin = findFloor(area.getMaxX(), height, area.getMinZ());
            int maxMax = findFloor(area.getMaxX(), height, area.getMaxZ());
            int minMin = findFloor(area.getMinX(), height, area.getMinZ());

            // Add corners first to override any other elements created by very small boundaries.
            addDisplayed(displayZone, new IntVector(area.getMinX(), minMax, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMax, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), minMin, area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMin, area.getMinZ()), addCorner);

            // North and south boundaries
            for (int x = Math.max(area.getMinX() + step, displayZone.getMinX()); x < area.getMaxX() - step / 2 && x < displayZone.getMaxX(); x += step) {
                addDisplayed(displayZone, new IntVector(x, findFloor(x, height, area.getMaxZ()), area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, findFloor(x, height, area.getMinZ()), area.getMinZ()), addSide);
            }

            // East and west boundaries
            for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), findFloor(area.getMinX(), height, z), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), findFloor(area.getMaxX(), height, z), z), addSide);
            }

            // First and last step are always directly adjacent to corners
            if (area.getLength() > 2) {
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, minMax, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, minMin, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, maxMax, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, maxMin, area.getMinZ()), addSide);
            }

            if (area.getWidth() > 2) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), minMin, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMin, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), minMax, area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), maxMax, area.getMaxZ() - 1), addSide);
            }
        }
    }

    /**
     * Create a {@link Consumer} that adds a corner element for the given {@link IntVector}.
     *
     * @param boundary the {@code Boundary}
     * @return the corner element consumer
     */
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return intVector -> {};
    }

    /**
     * Create a {@link Consumer} that adds a side element for the given {@link IntVector}.
     *
     * @param boundary the {@code Boundary}
     * @return the side element consumer
     */
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return intVector -> {};
    }

    protected boolean isAccessible(@NotNull BoundingBox displayZone, @NotNull IntVector coordinate)
    {
        return displayZone.contains2d(coordinate) && coordinate.isChunkLoaded(world);
    }

    /**
     * Checks for a valid floor traversing up and down between y - 64 and y + 16<br>
     * You may override this method to change the height limits used by the draw method as it calls this method.
     * @param x the x coordinate
     * @param y the starting y coordinate
     * @param z the z coordinate
     * @return the Y coordinate of the floor or y - 2 if no floor is found
     * @see #findFloor(World, int, int, int, int, int, int)
     * @see #isValidFloor(World, int, int, int, int)
     */
    public int findFloor(int x, int y, int z) {
        return findFloor(world, x, y, z, Math.max(worldMinHeight, y - 90), Math.min(worldMaxHeight, y + 16), y - 2);
    }

    /**
     * Checks for a valid floor traversing up and down within the specified limits
     * @param world the world to search in
     * @param x the x coordinate
     * @param y the starting y coordinate
     * @param z the z coordinate
     * @param minY the minimum Y value that will be searched
     * @param maxY the maximum Y value that will be searched
     * @param def the default return value if no floor is found
     * @return the Y coordinate of the floor or def if no floor is found
     * @see #isValidFloor(World, int, int, int, int)
     * @see #isValidFloor(Block)
     */
    public int findFloor(World world, int x, int y, int z, int minY, int maxY, int def) {
        if (isValidFloor(world, y, x, y, z)) return y;
        // search up and down within the specified limits
        int ly = y, gy = y;
        int maxAbs = Math.max(Math.abs(minY), Math.abs(maxY));
        for (int i = 0; i < maxAbs; i++) {
            if (ly > minY && isValidFloor(world, y, x, --ly, z)) return ly;
            if (gy < maxY && isValidFloor(world, y, x, ++gy, z)) return gy;
        }
        // if no floor is found return the default value
        return def;
    }

    /**
     * Returns if the coordinates are considered a valid floor by the {@link #findFloor(World, int, int, int, int, int, int) findFloor} method<br>
     * Override this method to define your own floor detection
     * @param world the world being checked
     * @param originalY the original Y coordinate of the search
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the coordinates are considered a valid floor, false otherwise
     * @see #isValidFloor(Block)
     * @see #findFloor(World, int, int, int, int, int, int)
     */
    public boolean isValidFloor(World world, int originalY, int x, int y, int z) {
        return isValidFloor(world.getBlockAt(x, y, z));
    }

    /**
     * Returns if the block is considered a valid floor by the {@link #findFloor(World, int, int, int, int, int, int) findFloor} method<br>
     * @param block the block to check
     * @return true if the block is considered a valid floor, false otherwise
     * @see #isValidFloor(World, int, int, int, int)
     * @see #findFloor(World, int, int, int, int, int, int)
     */
    public boolean isValidFloor(Block block) {
        return true;
    }

    /**
     * Add a display element if accessible.
     *
     * @param displayZone the zone in which elements may be displayed
     * @param coordinate the coordinate being displayed
     * @param addElement the function for obtaining the element displayed
     */
    protected void addDisplayed(
            @NotNull BoundingBox displayZone,
            @NotNull IntVector coordinate,
            @NotNull Consumer<@NotNull IntVector> addElement)
    {
        if (isAccessible(displayZone, coordinate)) {
            addElement.accept(coordinate);
        }
    }

    @Override
    public void revert(@Nullable Player player)
    {
        // If the player cannot visualize the blocks, they should already be effectively reverted.
        if (!canVisualize(player))
        {
            return;
        }

        // Elements do not track the boundary they're attached to - all elements are reverted individually instead.
        this.elements.forEach(element -> element.erase(player, world));
    }

    @Override
    protected void erase(@NotNull Player player, @NotNull Boundary boundary)
    {
        this.elements.forEach(element -> element.erase(player, world));
    }

}
