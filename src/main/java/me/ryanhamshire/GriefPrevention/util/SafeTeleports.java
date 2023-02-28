package me.ryanhamshire.GriefPrevention.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * This is a class from my me.taucu.BukkitUtils library with some modifications<br>
 * It is responsible for finding safe locations to teleport players to.
 * @author Tau
 */
public class SafeTeleports {

    /**
     * Uses magic to find the nearest safe location to teleport someone to.
     * @param from the location to start the check
     * @param box the bounding box of the player
     * @param maxRadius the max radius of the search
     * @param minY the min Y to search
     * @param maxY the max Y to search
     * @param predicate the predicate to perform more checks
     * @return a location if a safe one is found, otherwise null
     */
    public static Location findNearestSafeLocation(Location from, BoundingBox box, int maxRadius, int minY, int maxY, Predicate<Block> predicate) {
        World w = from.getWorld();
        Block block = from.getBlock();

        int by = block.getY();
        int bx = block.getX();
        int bz = block.getZ();

        Block checkBlock = null;

        int py = by, ny = by;
        ny--;

        do {
            if (ny >= minY) {
                checkBlock = scanXZ(w, maxRadius, bx, ny, bz, predicate);
                ny--;
            }
            if (py <= maxY && checkBlock == null) {
                checkBlock = scanXZ(w, maxRadius, bx, py, bz, predicate);
                py++;
            }
        } while ((py <= maxY || ny >= minY) && checkBlock == null);

        if (checkBlock != null) {
            return resolveTopBlockTpLocation(checkBlock, box);
        }
        return null;
    }

    /**
     * Scans the XZ around a position for valid blocks to teleport to<br>
     * @param w the world to check in
     * @param maxRadius the max radius to search
     * @param bx the starting X coordinate
     * @param by the Y coordinate
     * @param bz the starting Z coordinate
     * @param predicate the predicate to perform more checks
     * @return the block or null if no safe block is found
     */
    public static Block scanXZ(World w, int maxRadius, int bx, int by, int bz, Predicate<Block> predicate) {
        Block checkBlock = null;
        for (int radius = 1; radius < maxRadius && checkBlock == null; radius++) {
            for (int i = -radius; i <= radius && checkBlock == null; i++) {
                checkBlock = isGoodOrNull(w.getBlockAt(bx - radius, by, bz + i), predicate);
            }
            for (int i = -radius; i <= radius && checkBlock == null; i++) {
                checkBlock = isGoodOrNull(w.getBlockAt(bx + radius, by, bz - i), predicate);
            }
            for (int i = -radius; i <= radius && checkBlock == null; i++) {
                checkBlock = isGoodOrNull(w.getBlockAt(bx + i, by, bz - radius), predicate);
            }
            for (int i = -radius; i <= radius && checkBlock == null; i++) {
                checkBlock = isGoodOrNull(w.getBlockAt(bx - i, by, bz + radius), predicate);
            }
        }
        return checkBlock;
    }

    /**
     * Finds a safe location "around" a bounding box. This method will continue searching until the maxVolume has been reached
     * @param world the world to search in
     * @param box the bounding box of the player
     * @param around the bounding box to search around
     * @param maxHeight the maximum height to search
     * @param minHeight the minimum height to search
     * @param maxCheckedBlocks the maximum number of blocks check
     * @param predicate the predicate for additional filtering
     * @return the location, or null if no safe location is found
     */
    public static Location findSafeLocationAround(World world, BoundingBox box, me.ryanhamshire.GriefPrevention.util.BoundingBox around, int minHeight, int maxHeight, long maxCheckedBlocks, Predicate<Block> predicate) {
        // one bounding box for each face of the "around" box. we will shift these boxes and expand them by one each interaction
        // this will effectively create a pyramid coming out of each face of the "around" box.
        // this is much more efficient than checking if each block is inside-of the "around" box, as the "around" box could be quite large.
        me.ryanhamshire.GriefPrevention.util.BoundingBox lx, ly, lz, gx, gy, gz;
        int minY = Math.max(minHeight, around.getMinY());
        int maxY = Math.min(maxHeight, around.getMaxY());
        lx = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMinX(), minY, around.getMinZ(), around.getMinX(), maxY, around.getMaxZ());
        ly = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMinX(), minY, around.getMinZ(), around.getMaxX(), minY, around.getMaxZ());
        lz = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMinX(), minY, around.getMinZ(), around.getMaxX(), maxY, around.getMinZ());

        gx = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMaxX(), minY, around.getMinZ(), around.getMaxX(), maxY, around.getMaxZ());
        gy = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMinX(), maxY, around.getMinZ(), around.getMaxX(), maxY, around.getMaxZ());
        gz = new me.ryanhamshire.GriefPrevention.util.BoundingBox(around.getMinX(), minY, around.getMaxZ(), around.getMaxX(), maxY, around.getMaxZ());

        // teleporting players outside the WorldBorder is rude.
        me.ryanhamshire.GriefPrevention.util.BoundingBox borderBox = me.ryanhamshire.GriefPrevention.util.BoundingBox.of(world.getWorldBorder(), minHeight, maxHeight);
        long[] checkedBlocks = new long[] {0L};
        do {
            lx.move(-1, 0, 0);
            if (ly != null) ly.move(0, -1, 0);
            lz.move(0, 0, -1);

            gx.move(1, 0, 0);
            if (gy != null) gy.move(0, 1, 0);
            gz.move(0, 0, 1);

            lx.expand(0, lx.getMinY() >= minHeight ? 0 : 1, 1, 0, lx.getMaxY() <= maxHeight ? 0 : 1, 1);
            if (ly != null) ly.expand(1, 0, 1, 1, 0, 1);
            lz.expand(1, lz.getMinY() >= minHeight ? 0 : 1, 0, 1, lz.getMaxY() <= maxHeight ? 0 : 1, 0);

            gx.expand(0, gx.getMaxY() <= maxHeight ? 0 : 1, 1, 0, gx.getMinY() >= minHeight ? 0 : 1, 1);
            if (gy != null) gy.expand(1, 0, 1, 1, 0, 1);
            gz.expand(1, gz.getMaxY() <= maxHeight ? 0 : 1, 0, 1, gz.getMinY() >= minHeight ? 0 : 1, 0);

            // reached world min/max height, don't compute any longer.
            if (ly != null && ly.getMinY() < minHeight) ly = null;
            if (gy != null && gy.getMaxY() > maxHeight) gy = null;

            Block block;
            if ((block = checkAABB(world, lx, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null
                    || ly != null && (block = checkAABB(world, ly, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null
                    || (block = checkAABB(world, lz, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null
                    || (block = checkAABB(world, gx, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null
                    || gy != null && (block = checkAABB(world, gy, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null
                    || (block = checkAABB(world, gz, borderBox, checkedBlocks, maxCheckedBlocks, predicate)) != null) {
                return resolveTopBlockTpLocation(block, box);
            }

        } while (checkedBlocks[0] <= maxCheckedBlocks);

        return null;
    }

    private static Block checkAABB(World world, me.ryanhamshire.GriefPrevention.util.BoundingBox check, me.ryanhamshire.GriefPrevention.util.BoundingBox border, long[] checkedBlocks, long maxCheckedBlocks, Predicate<Block> predicate) {
        // make sure the first time this runs that cx and cz are not == x >> 4 and z >> 4
        int cx = check.getMinX() + 1, cz = check.getMinZ() + 1;
        for (int x = check.getMinX(); x <= check.getMaxX() && x >= border.getMinX() && x <= border.getMaxX(); x++) {
            for (int z = check.getMinZ(); z <= check.getMaxZ() && z >= border.getMinZ() && z <= border.getMaxZ(); z++) {
                // only check if chunk is loaded
                if ((cx == x >> 4 && cz == z >> 4) || world.isChunkLoaded((cx = x >> 4), (cz = z >> 4))) {
                    // search from maxY down
                    for (int y = check.getMaxY(); y >= check.getMinY() && y >= border.getMinY() && y <= border.getMaxY(); y--) {
                        if (++checkedBlocks[0] > maxCheckedBlocks) return null;
                        Block test = isGoodOrNull(world.getBlockAt(x, y, z), predicate);
                        if (test != null) {
                            return test;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param check the block to check
     * @param predicate the predicate to perform more checks
     * @return null if {@link #isBad(Block)} or the check argument
     */
    public static Block isGoodOrNull(Block check, Predicate<Block> predicate) {
        return !isBad(check) && predicate.test(check) ? check : null;
    }

    /**
     * Checks if a bounding box intersects any block considered to be detrimental to the health of the player<br>
     * Like lava, fire, solid blocks, if there is a supportive block below etc.
     * @param box the box to check
     * @param w the world to check in
     * @return true if the bounding box intersects something "bad"
     */
    public static boolean intersectsBad(BoundingBox box, World w) {
        int blx = NumberConversions.floor(box.getMinX()),
                bgx = NumberConversions.ceil(box.getMaxX()),
                bly = NumberConversions.floor(box.getMinY()),
                bgy = NumberConversions.ceil(box.getMaxY()),
                blz = NumberConversions.floor(box.getMinZ()),
                bgz = NumberConversions.ceil(box.getMaxZ());
        for (int x = blx; x < bgx; x++) {
            for (int y = bly; y < bgy; y++) {
                for (int z = blz; z < bgz; z++) {
                    Block check = w.getBlockAt(x, y, z);
                    if (badMaterialSwitch(check.getType())) {
                        return true;
                    } else if (!check.isPassable()) {
                        for (BoundingBox blockBox : check.getCollisionShape().getBoundingBoxes()) {
                            // relative or not relative, bukkit? Choose one or the other.
                            blockBox.shift(check.getLocation());
                            if (box.overlaps(blockBox)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a block is a bad place to teleport a player.<br>
     * It will check for things like lava, fire, solid blocks, if there is a supportive block below etc.
     * @param block The block to check (block the player will be above)
     * @return true if it's a crappy place to teleport someone
     */
    public static boolean isBad(Block block) {
        Material type = block.getType();
        // isPassable is an intensive check, check if air first
        return (type.isAir() || block.isPassable() && !(type == Material.WATER && isGoodWaterBelow(block))) || badMaterialSwitch(type) || isBadAbove(block);
    }

    /**
     * Broken out logic to check if a material is "bad", like lava, fire, and all things (not) nice.
     * @param type the type to check
     * @return if it's bad.
     */
    public static boolean badMaterialSwitch(Material type) {
        switch (type) {
            case LAVA:
            case MAGMA_BLOCK:
            case FIRE:
            case SOUL_FIRE:
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
            case WITHER_ROSE:
                return true;
            default:
                return false;
        }
    }

    /**
     * It's only good water if it's 4 or more water blocks thick. Otherwise, players could fall through.
     * @param block the block to check
     * @return if it's "supportive" water
     */
    public static boolean isGoodWaterBelow(Block block) {
        for (int i = 0; i < 4; i++) {
            block = block.getRelative(BlockFace.DOWN);
            if (block.getType() != Material.WATER && isBad(block)) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks if the blocks above a player are "bad"<br>
     * Think lava, fire solid blocks etc.
     *
     * @param block the block to check
     * @return true if bad false otherwise
     */
    public static boolean isBadAbove(Block block) {
        BoundingBox box = getHighestBoundingBoxOf(block);
        int start,
                stop;
        if (box == null) {
            start = 1;
            stop = 3;
        } else {
            start = Math.max(1, NumberConversions.ceil(box.getMaxY()));
            stop = 2 + start;
        }
        for (int i = start; i < stop; i++) {
            Block check = block.getRelative(0, i, 0);
            if (badMaterialSwitch(check.getType()) || !check.isPassable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines the best location on a block that will support a player (think open trapdoors)
     *
     * @param b   the block to resolve
     * @param box the players bounding box
     * @return an exact location to teleport the player
     */
    public static Location resolveTopBlockTpLocation(Block b, BoundingBox box) {
        box = box.clone();
        // so, bukkit. do you like relative or non-relative? Please decide.
        box.shift(-box.getCenterX(), -box.getCenterY(), -box.getCenterZ());
        BoundingBox bbox = getHighestBoundingBoxOf(b);
        if (bbox == null) {
            return b.getLocation().add(0.5, 0.5, 0.5);
        }
        Vector center = bbox.getCenter();

        Location loc;
        if (center == null) {
            loc = b.getLocation().add(0.5, 1, 0.5);
        } else {
            center.setY(bbox.getMaxY());
            // shift location to accommodate player hitbox
            if (center.getX() + box.getMaxX() > 1) {
                center.setX(1 - box.getMaxX());
            } else if (center.getX() + box.getMinX() < 0) {
                center.setX(0 - box.getMinX());
            }

            if (center.getZ() + box.getMaxZ() > 1) {
                center.setZ(1 - box.getMaxZ());
            } else if (center.getZ() + box.getMinZ() < 0) {
                center.setZ(0 - box.getMinZ());
            }

            loc = center.toLocation(b.getWorld()).add(b.getX(), b.getY(), b.getZ());
        }

        return loc;
    }

    public static BoundingBox getHighestBoundingBoxOf(Block block) {
        double maxHeight = 0;
        BoundingBox highest = null;
        for (BoundingBox bbox : block.getCollisionShape().getBoundingBoxes()) {
            if (bbox.getMaxY() > maxHeight) {
                maxHeight = bbox.getMaxY();
                highest = bbox;
            }
        }
        return highest;
    }

}

