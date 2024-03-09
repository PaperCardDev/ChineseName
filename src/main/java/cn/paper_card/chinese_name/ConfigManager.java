package cn.paper_card.chinese_name;

import org.jetbrains.annotations.NotNull;

class ConfigManager {
    private final @NotNull ThePlugin plugin;
    private final @NotNull String path_coins_for_normal = "coins-for-normal";
    private final @NotNull String path_coins_for_special = "coins-for-special";

    ConfigManager(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
    }

    long getCoinsForNormal() {
        return this.plugin.getConfig().getLong(path_coins_for_normal, 30);
    }

    void setCoinsForNormal(long v) {
        this.plugin.getConfig().set(path_coins_for_normal, v);
    }

    long getCoinsForSpecial() {
        return this.plugin.getConfig().getLong(path_coins_for_special, 50);
    }

    void setCoinsForSpecial(long v) {
        this.plugin.getConfig().set(path_coins_for_special, v);
    }

    void setDefaults() {
        this.setCoinsForNormal(this.getCoinsForNormal());
        this.setCoinsForSpecial(this.getCoinsForSpecial());
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }
}
