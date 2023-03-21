package com.griefprevention.visualization.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.FakeEntityElement;
import me.ryanhamshire.GriefPrevention.util.DataWatchers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class FakeShulkerBulletElement extends FakeEntityElement {

    private String teamName;

    public FakeShulkerBulletElement(IntVector vector, Team teamColor) {
        super(vector);
        this.teamName = teamColor.getName();
    }

    @Override
    protected void draw() {
        IntVector pos = getCoordinate();

        // spawn shulker bullet
        PacketContainer addEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        addEntity.getEntityTypeModifier().write(0, EntityType.SHULKER_BULLET); // the type to spawn
        addEntity.getIntegers().write(0, entityId); // entityId
        addEntity.getUUIDs().write(0, entityUid); // the UUID of the entity
        addEntity.getDoubles()
                .write(0, pos.x() + 0.5) // the X of the entity
                .write(1, pos.y() + 0.34375) // the Y of the entity
                .write(2, pos.z() + 0.5);// the Z of the entity

        // make the bullet glow and have no gravity
        PacketContainer entityMeta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        entityMeta.getIntegers().write(0, entityId); // the target entityId
        entityMeta.getDataValueCollectionModifier().write(0, List.of(DataWatchers.NO_GRAVITY, DataWatchers.GLOWING)); // make it have no gravity and glow

        // add the bullet to the scoreboard team to give color to the glow effect
        PacketContainer addToTeam = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        addToTeam.getIntegers().write(0, 3); // add to team action
        addToTeam.getStrings().write(0, teamName); // team name
        addToTeam.getSpecificModifier(Collection.class).write(0, List.of(entityUid.toString())); // UUIDs of team members to add

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addEntity);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, entityMeta);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addToTeam);
    }

    public static void eraseAllBullets(Player whom, Collection<? extends FakeShulkerBulletElement> elements) {
        if (!elements.isEmpty()) {
            int teamAllocSize = elements.size();
            Map<String, ArrayList<String>> teamToUUID = new HashMap<>();
            for (FakeShulkerBulletElement element : elements) {
                if (element.drawn()) {
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
