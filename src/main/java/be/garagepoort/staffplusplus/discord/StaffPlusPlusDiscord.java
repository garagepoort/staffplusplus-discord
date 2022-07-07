package be.garagepoort.staffplusplus.discord;

import be.garagepoort.mcioc.tubingbukkit.TubingBukkitPlugin;
import be.garagepoort.staffplusplus.discord.common.PluginDisable;

public class StaffPlusPlusDiscord extends TubingBukkitPlugin {

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
