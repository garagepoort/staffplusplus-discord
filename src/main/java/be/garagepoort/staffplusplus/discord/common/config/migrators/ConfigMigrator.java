package be.garagepoort.staffplusplus.discord.common.config.migrators;

import org.bukkit.configuration.file.FileConfiguration;

public interface ConfigMigrator {

    void migrate(FileConfiguration config);
}
