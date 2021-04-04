package be.garagepoort.staffplusplus.discord.common.config.migrators;

import org.bukkit.configuration.file.FileConfiguration;

public class ReportsConfigMigrator implements ConfigMigrator {
    @Override
    public void migrate(FileConfiguration config) {
        migrate(config, "StaffPlusPlusDiscord.webhookUrl");
        migrate(config, "StaffPlusPlusDiscord.notifyOpen");
        migrate(config, "StaffPlusPlusDiscord.notifyReopen");
        migrate(config, "StaffPlusPlusDiscord.notifyAccept");
        migrate(config, "StaffPlusPlusDiscord.notifyReject");
        migrate(config, "StaffPlusPlusDiscord.notifyResolve");
    }

    private void migrate(FileConfiguration config, String s) {
        Object value = config.get(s, null);
        String key = s.substring(s.lastIndexOf(".") + 1);
        if (value != null) {
            config.set("StaffPlusPlusDiscord.reports." + key, value);
        }
        config.set(s,null);
    }
}
