package com.griefprevention.visualization.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.BlockElement;
import me.ryanhamshire.GriefPrevention.util.ProtocolUtil;
import me.ryanhamshire.GriefPrevention.util.UUIDUtil;
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
import java.util.Map.Entry;

public class FakeFallingBlockElement extends BlockElement {

    private static final HashMap<BlockData, Boolean> INHERIT_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    private static final List<WrappedDataValue> DATAWATCHERS = List.of(
            new WrappedDataValue(0, Registry.get(Byte.class), (byte) 0x40), // status: set glowing
            new WrappedDataValue(5, Registry.get(Boolean.class), true) // noGravity: true
    );

    BlockData blockData;
    String teamName;

    int entityId = -1;
    UUID entityUid = null;
    boolean drawn = false;
    boolean changedBlock = false;

    public FakeFallingBlockElement(IntVector vector, @NotNull Team teamColor, BlockData blockData) {
        super(vector);
        this.teamName = teamColor.getName();
        this.blockData = blockData;
    }

    @Override
    public void draw(@NotNull Player player, @NotNull World world) {
        if (drawn()) return;
        this.entityId = ProtocolUtil.nextEntityId();
        this.entityUid = UUIDUtil.fastRandomUUID();
        this.drawn = true;

        IntVector pos = getCoordinate();
        BlockData fakeData = blockData;

        // if it is a full block, and it is not leaves, then we should use the same data as it
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

        PacketContainer addEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        addEntity.getEntityTypeModifier().write(0, EntityType.FALLING_BLOCK);
        addEntity.getIntegers()
                .write(0, entityId) // entityId
                .write(4, ProtocolUtil.getBlockStateId(fakeData)); // entityData in this case the ID of the blockstate
        addEntity.getUUIDs().write(0, entityUid);
        addEntity.getDoubles()
                .write(0, pos.x() + 0.5)
                .write(1, pos.y() + 0.0)
                .write(2, pos.z() + 0.5);

        PacketContainer entityMeta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        entityMeta.getIntegers().write(0, entityId);
        entityMeta.getDataValueCollectionModifier().write(0, DATAWATCHERS);

        PacketContainer addToTeam = new PacketContainer(Server.SCOREBOARD_TEAM);
        addToTeam.getIntegers().write(0, 3); // add
        addToTeam.getStrings().write(0, teamName); // team name
        addToTeam.getSpecificModifier(Collection.class).write(0, List.of(entityUid.toString())); // UUIDs

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addEntity);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, entityMeta);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addToTeam);
    }

    @Override
    public void erase(@NotNull Player player, @NotNull World world) {
        if (drawn()) {
            eraseAll(player, List.of(this));
        }
    }

    protected int invalidate() {
        drawn = false;
        changedBlock = false;
        return entityId;
    }

    public boolean drawn() {
        return drawn;
    }

    public int entityId() {
        return entityId;
    }

    // optimized erase method
    protected static void eraseAll(Player whom, Collection<FakeFallingBlockElement> elements) {
        if (!elements.isEmpty()) {
            int teamAllocSize = elements.size();
            List<Integer> toDestroy = new ArrayList<>(teamAllocSize);
            Map<String, ArrayList<String>> teamToUUID = new HashMap<>(4);
            for (FakeFallingBlockElement element : elements) {
                if (element.drawn()) {
                    // revert faked block (if any) with the real block at that location
                    if (element.changedBlock) {
                        Location location = element.getCoordinate().toLocation(whom.getWorld());
                        whom.sendBlockChange(location, location.getBlock().getBlockData());
                    }

                    // add each element entityID so that a single packet can destroy all client entities in this list
                    toDestroy.add(element.invalidate());

                    // now we sort each element UUID into a list based on their team (glowing color)
                    ArrayList<String> uuids = teamToUUID.get(element.teamName);
                    if (uuids == null) {
                        uuids = new ArrayList<>(teamAllocSize);
                        teamToUUID.put(element.teamName, uuids);
                    }
                    uuids.add(element.entityUid.toString());
                    teamAllocSize--;
                }
            }

            ProtocolUtil.destroyEntitiesFor(whom, toDestroy);

            // now that we have sorted each element UUID by its team, we can bulk remove them.
            for (Entry<String, ArrayList<String>> entry : teamToUUID.entrySet()) {
                PacketContainer teamPacket = new PacketContainer(Server.SCOREBOARD_TEAM);
                teamPacket.getIntegers().write(0, 4); // remove
                teamPacket.getStrings().write(0, entry.getKey()); // team name
                teamPacket.getSpecificModifier(Collection.class).write(0, entry.getValue()); // element UUIDs to remove
                ProtocolLibrary.getProtocolManager().sendServerPacket(whom, teamPacket);
            }

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

}
