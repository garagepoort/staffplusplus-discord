package be.garagepoort.staffplusplus.discord.common;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMulti;
import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import org.bukkit.Bukkit;

import java.util.List;

@IocBean
public class StaffPlusPlusListenerInitializer {

    public StaffPlusPlusListenerInitializer(@IocMulti(StaffPlusPlusListener.class) List<StaffPlusPlusListener> listeners) {
        try {
            listeners.forEach(this::initListener);
        } catch (RuntimeException e) {
            showError("Cannot enable StaffPlusPlusDiscord. " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(StaffPlusPlusDiscord.get());
        }
    }

    private void initListener(StaffPlusPlusListener listener) {
        if (listener.isEnabled()) {
            listener.validate();
            listener.init();
            StaffPlusPlusDiscord.get().getServer().getPluginManager().registerEvents(listener, StaffPlusPlusDiscord.get());
        }
    }

    private void showError(String errorMessage) {
        StaffPlusPlusDiscord.get().getLogger().severe("=============================================================================================");
        StaffPlusPlusDiscord.get().getLogger().severe("!!!  " + errorMessage);
        StaffPlusPlusDiscord.get().getLogger().severe("=============================================================================================");
    }
}
