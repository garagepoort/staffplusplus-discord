package be.garagepoort.staffplusplus.discord.common;

import org.bukkit.event.Listener;

public interface StaffPlusPlusListener extends Listener {

    void init();

    default void teardown() {}

    boolean isEnabled();

    void validate();
}
