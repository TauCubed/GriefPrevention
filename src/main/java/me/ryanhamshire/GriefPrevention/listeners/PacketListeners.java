package me.ryanhamshire.GriefPrevention.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.griefprevention.visualization.EntityBlockBoundaryVisualization;
import com.griefprevention.visualization.FakeEntityElement;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

public class PacketListeners {

    private final PacketListener useEntityListener;

    public PacketListeners() {
        this.useEntityListener = new PacketAdapter(GriefPrevention.instance, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PlayerData data = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getUniqueId());
                if (data.getVisibleBoundaries() instanceof EntityBlockBoundaryVisualization<?> vis) {
                    FakeEntityElement element = vis.elementByEID(event.getPacket().getIntegers().read(0));
                    if (element != null) {
                        element.erase(event.getPlayer(), vis.getWorld());
                        event.setCancelled(true);
                    }
                }
            }
        };
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(useEntityListener);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(useEntityListener);
    }

}
