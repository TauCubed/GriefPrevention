package me.ryanhamshire.GriefPrevention.registry;

import com.griefprevention.visualization.VisualizationProvider;
import com.griefprevention.visualization.VisualizationProviders;
import com.griefprevention.visualization.impl.AntiCheatCompatVisualization;
import com.griefprevention.visualization.impl.FakeBlockVisualization;
import com.griefprevention.visualization.impl.FakeFallingBlockVisualization;
import com.griefprevention.visualization.impl.FakeShulkerBulletVisualization;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class Registries {

    public static final Registry<Registry<?>> REGISTRIES = new Registry<>("greifprevention:registries");

    public static final DefaultedRegistry<VisualizationProvider> VISUALIZATION_PROVIDERS = new DefaultedRegistry<>("griefprevention:visualization_providers", (world, visualizeFrom, height) -> {
        if (GriefPrevention.instance.support_protocollib_enabled) {
            return new FakeFallingBlockVisualization(world, visualizeFrom, height);
        } else {
            return new FakeBlockVisualization(world, visualizeFrom, height);
        }
    });

    // boostrap
    static {
        // add registries
        REGISTRIES.register(VISUALIZATION_PROVIDERS.getName(), VISUALIZATION_PROVIDERS);

        // init registries
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_FALLING_BLOCK.getKey(), FakeFallingBlockVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_SHULKER_BULLET.getKey(), FakeShulkerBulletVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK.getKey(), FakeBlockVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK_ANTI_CHEAT_COMPAT.getKey(), AntiCheatCompatVisualization::new);
    }

}
