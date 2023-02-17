package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

public abstract class BlockBoundaryVisualization extends BoundaryVisualization
{

    protected final int step;
    protected final BoundingBox displayZoneArea;
    protected final Collection<BlockElement> elements = new HashSet<>();

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

        // add to displayed boundaries
        boundaries.add(boundary);

        boolean is3d = area.getMaxY() < Claim._2D_HEIGHT;
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
                addDisplayed(displayZone, new IntVector(x, height, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(x, height, area.getMinZ()), addSide);
            }
        }
        // First and last step are always directly adjacent to corners
        if (area.getLength() > 2)
        {
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
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, height, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX() + 1, height, area.getMinZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, height, area.getMaxZ()), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX() - 1, height, area.getMinZ()), addSide);
            }
        }

        // East and west boundaries
        for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step)
        {
            if (is3d) {
                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMaxY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMaxY(), z), addSide);

                addDisplayed(displayZone, new IntVector(area.getMinX(), area.getMinY(), z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), area.getMinY(), z), addSide);
            } else {
                addDisplayed(displayZone, new IntVector(area.getMinX(), height, z), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), height, z), addSide);
            }
        }
        if (area.getWidth() > 2)
        {
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
                addDisplayed(displayZone, new IntVector(area.getMinX(), height, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), height, area.getMinZ() + 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMinX(), height, area.getMaxZ() - 1), addSide);
                addDisplayed(displayZone, new IntVector(area.getMaxX(), height, area.getMaxZ() - 1), addSide);
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
            addDisplayed(displayZone, new IntVector(area.getMinX(), height, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), height, area.getMaxZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMinX(), height, area.getMinZ()), addCorner);
            addDisplayed(displayZone, new IntVector(area.getMaxX(), height, area.getMinZ()), addCorner);
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

    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary, boolean literalHeight) {
        return addCornerElements(boundary);
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

    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary, boolean literalHeight) {
        return addSideElements(boundary);
    }

    protected boolean isAccessible(@NotNull BoundingBox displayZone, @NotNull IntVector coordinate)
    {
        return displayZone.contains2d(coordinate) && coordinate.isChunkLoaded(world);
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
