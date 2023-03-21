package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.util.ProtocolUtil;
import me.ryanhamshire.GriefPrevention.util.UUIDUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class FakeEntityElement extends BlockElement {

    protected Player player = null;
    protected World world = null;
    protected int entityId = -1;
    protected UUID entityUid = null;
    protected boolean drawn = false;

    public FakeEntityElement(IntVector vector) {
        super(vector);
    }

    @Override
    public void draw(@NotNull Player player, @NotNull World world) {
        if (drawn()) return;
        this.player = player;
        this.world = world;
        this.entityId = ProtocolUtil.nextEntityId();
        this.entityUid = UUIDUtil.fastRandomUUID(ThreadLocalRandom.current());
        this.drawn = true;
        draw();
    }

    protected abstract void draw();

    @Override
    public void erase(@NotNull Player player, @NotNull World world) {
        if (drawn()) {
            eraseAllEntities(player, List.of(this));
        }
    }

    protected void onErase() {
        player = null;
        world = null;
        entityId = -1;
        entityUid = null;
        drawn = false;
    }

    public boolean drawn() {
        return drawn;
    }

    public int entityId() {
        return entityId;
    }

    public UUID entityUID() {
        return entityUid;
    }

    // optimized erase method
    public static void eraseAllEntities(Player whom, Collection<? extends FakeEntityElement> elements) {
        if (!elements.isEmpty()) {
            List<Integer> toDestroy = new ArrayList<>(elements.size());
            for (FakeEntityElement element : elements) {
                toDestroy.add(element.entityId());
                element.onErase();
            }

            ProtocolUtil.destroyEntitiesFor(whom, toDestroy);
        }
    }

}
