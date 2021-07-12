package be.garagepoort.staffplusplus.discord;

import be.garagepoort.mcioc.TubingPlugin;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.config.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class StaffPlusPlusDiscord extends TubingPlugin {

    private static StaffPlusPlusDiscord plugin;
    private static final List<StaffPlusPlusListener> LISTENERS = new ArrayList<>();

    public static StaffPlusPlusDiscord get() {
        return plugin;
    }


    @Override
    protected void enable() {
        plugin = this;
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
    protected void disable() {
        getLogger().info("StaffPlusPlusDiscord plugin disabled");
    }

}
