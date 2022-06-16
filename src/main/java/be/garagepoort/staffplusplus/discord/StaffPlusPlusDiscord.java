package be.garagepoort.staffplusplus.discord;

import be.garagepoort.mcioc.TubingPlugin;
import be.garagepoort.staffplusplus.discord.common.PluginDisable;
import be.garagepoort.staffplusplus.discord.common.config.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class StaffPlusPlusDiscord extends TubingPlugin {

    private static StaffPlusPlusDiscord plugin;

    public static StaffPlusPlusDiscord get() {
        return plugin;
    }

    @Override
    protected void beforeEnable() {
        plugin = this;
    }

    @Override
    protected void enable() {
        getLogger().info("StaffPlusPlusDiscord plugin enabled");
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        ConfigUpdater.updateConfig(this);
        if (!ConfigUpdater.updateConfig(this)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    protected void beforeReload() {
        getIocContainer().getList(PluginDisable.class).forEach(b -> b.disable(this));
    }

    @Override
    protected void disable() {
        getLogger().info("StaffPlusPlusDiscord plugin disabled");
    }

}
