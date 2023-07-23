package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.EntityBlockBoundaryVisualization;
import me.ryanhamshire.GriefPrevention.util.ScoreboardColors;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FakeShulkerBulletVisualization extends EntityBlockBoundaryVisualization<FakeShulkerBulletElement> {

    public FakeShulkerBulletVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height) {
        super(world, visualizeFrom, height);
    }

    public FakeShulkerBulletVisualization(World world, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(world, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return switch (boundary.type()) {
            case ADMIN_CLAIM ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.GOLD));
            case SUBDIVISION ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.WHITE));
            case INITIALIZE_ZONE, NATURE_RESTORATION_ZONE ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.AQUA));
            case CONFLICT_ZONE ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.RED));
            default ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.YELLOW));
        };
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return addCornerElements(boundary);
    }

    protected @NotNull Consumer<@NotNull IntVector> addBulletElement(@NotNull Team teamColor) {
        return vector -> {
            // don't draw over existing elements in the same position
            entityElements.putIfAbsent(vector, new FakeShulkerBulletElement(vector, teamColor));
        };
    }

    @Override
    public void revert(Player player) {
        if (player != null) {
            FakeShulkerBulletElement.eraseAllBullets(player, entityElements.values());
        }
    }

    @Override
    public boolean isValidFloor(World world, int originalY, int x, int y, int z) {
        return FakeFallingBlockVisualization.isFloor(world, originalY, x, y, z);
    }

    @Override
    public boolean isValidFloor(Block block) {
        throw new UnsupportedOperationException("not implemented. use isValidFloor(org.bukkit.World, int, int, int, int)");
    }

}
