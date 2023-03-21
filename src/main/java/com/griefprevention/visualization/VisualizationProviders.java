package com.griefprevention.visualization;

public enum VisualizationProviders {

    FAKE_FALLING_BLOCK("griefprevention:fake_falling_block"),
    FAKE_SHULKER_BULLET("griefprevention:fake_shulker_bullet"),
    FAKE_BLOCK("griefprevention:fake_block"),
    FAKE_BLOCK_ANTI_CHEAT_COMPAT("griefprevention:fake_block_anti_cheat_compat");

    private final String key;

    VisualizationProviders(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
