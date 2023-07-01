package com.griefprevention.visualization.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.util.DataWatchers;
import me.ryanhamshire.GriefPrevention.util.ProtocolUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Fake falling block visualization element for boundary visualization.<br>
 * Used to spawn fake falling blocks that have the glowing effect and can be added to specified scoreboard teams
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public class FakeFallingBlockElement extends com.griefprevention.visualization.FakeEntityElement {

    private static final HashMap<BlockData, Boolean> INHERIT_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    String teamName;
    BlockData blockData;
    boolean changedBlock = false;

    public FakeFallingBlockElement(IntVector vector, Team teamColor, BlockData blockData) {
        super(vector);
        this.teamName = teamColor.getName();
        this.blockData = blockData;
    }

    @Override
    protected void draw() {
        IntVector pos = getCoordinate();
        BlockData fakeData = blockData;

        Block realBlock = world.getBlockAt(pos.x(), pos.y(), pos.z());
        BlockData realData = realBlock.getBlockData();

        if (shouldInheritBlockData(realBlock, realData)) {
            fakeData = realData;
        }

        // if the data is the same the client will destroy the falling block, so we must fake it in this situation
        if (realData.equals(fakeData)) {
            player.sendBlockChange(pos.toLocation(world), Material.BARRIER.createBlockData());
            changedBlock = true;
        }

        // spawn falling block
        PacketContainer addEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        addEntity.getEntityTypeModifier().write(0, EntityType.FALLING_BLOCK); // the type to spawn
        addEntity.getIntegers()
                .write(0, entityId) // entityId
                .write(4, ProtocolUtil.getBlockStateId(fakeData)); // entityData in this case the ID of the blockstate
        addEntity.getUUIDs().write(0, entityUid); // the UUID of the entity
        addEntity.getDoubles()
                .write(0, pos.x() + 0.5) // the X of the entity
                .write(1, pos.y() + 0.0) // the Y of the entity
                .write(2, pos.z() + 0.5);// the Z of the entity

        // make the falling blocks glow and have no gravity
        PacketContainer entityMeta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        entityMeta.getIntegers().write(0, entityId); // the target entityId
        entityMeta.getDataValueCollectionModifier().write(0, List.of(DataWatchers.NO_GRAVITY, DataWatchers.GLOWING)); // make it have no gravity and glow

        // add the blocks to the scoreboard team to give color to the glow effect
        PacketContainer addToTeam = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        addToTeam.getIntegers().write(0, 3); // add to team action
        addToTeam.getStrings().write(0, teamName); // team name
        addToTeam.getSpecificModifier(Collection.class).write(0, List.of(entityUid.toString())); // UUIDs of team members to add

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addEntity);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, entityMeta);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addToTeam);
    }

    @Override
    public void erase(@NotNull Player player, @NotNull World world) {
        if (drawn()) {
            eraseAllFallingBlocks(player, List.of(this));
        }
    }

    public static boolean shouldInheritBlockData(Block block, BlockData data) {
        Boolean isFullBlock = INHERIT_BLOCK_CACHE.get(data);
        if (isFullBlock == null) {
            isFullBlock = !block.isPassable()
                    && !Tag.LEAVES.isTagged(data.getMaterial())
                    && !data.getMaterial().toString().contains("GLASS")
                    && block.getCollisionShape().getBoundingBoxes().stream().mapToDouble(BoundingBox::getVolume).sum() == 1
                    && block.getBoundingBox().getVolume() == 1;
            // ensure that the same blockData is returned, otherwise this cache won't work and might memory leak
            if (block.getBlockData().hashCode() == block.getBlockData().hashCode()) INHERIT_BLOCK_CACHE.put(data, isFullBlock);
        }
        return isFullBlock;
    }

    public static void eraseAllFallingBlocks(Player whom, Collection<? extends FakeFallingBlockElement> elements) {
        if (!elements.isEmpty()) {
            int teamAllocSize = elements.size();
            Map<String, ArrayList<String>> teamToUUID = new HashMap<>();
            for (FakeFallingBlockElement element : elements) {
                if (element.drawn()) {
                    // revert faked block (if any) with the real block at that location
                    if (element.changedBlock) {
                        element.changedBlock = false;
                        Location location = element.getCoordinate().toLocation(whom.getWorld());
                        whom.sendBlockChange(location, location.getBlock().getBlockData());
                    }

                    // now we sort each element UUID into a list based on their team (glowing color)
                    ArrayList<String> uuids = teamToUUID.get(element.teamName);
                    if (uuids == null) {
                        uuids = new ArrayList<>(teamAllocSize);
                        teamToUUID.put(element.teamName, uuids);
                    }
                    uuids.add(element.entityUID().toString());
                    teamAllocSize--;
                }
            }

            // before we remove teams, remove the entities
            eraseAllEntities(whom, elements);

            // now that we have sorted each element UUID by its team, we can bulk remove them.
            for (Map.Entry<String, ArrayList<String>> entry : teamToUUID.entrySet()) {
                PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
                teamPacket.getIntegers().write(0, 4); // remove action
                teamPacket.getStrings().write(0, entry.getKey()); // team name
                teamPacket.getSpecificModifier(Collection.class).write(0, entry.getValue()); // UUIDs of team members to remove
                ProtocolLibrary.getProtocolManager().sendServerPacket(whom, teamPacket);
            }
        }
    }

}
