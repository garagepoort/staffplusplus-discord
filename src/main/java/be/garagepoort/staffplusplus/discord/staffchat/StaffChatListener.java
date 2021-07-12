package be.garagepoort.staffplusplus.discord.staffchat;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import github.scarsz.discordsrv.DiscordSRV;
import net.shortninja.staffplusplus.IStaffPlus;
import net.shortninja.staffplusplus.staffmode.chat.StaffChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.RegisteredServiceProvider;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class StaffChatListener implements StaffPlusPlusListener {
    @ConfigProperty("StaffPlusPlusDiscord.staffchat.sync")
    private boolean syncEnabled;

    private final StaffPlusPlusDiscord staffPlusPlusDiscord;
    private IStaffPlus staffPlus;
    private DiscordStaffChatListener discordListener;

    public StaffChatListener() {
        this.staffPlusPlusDiscord = StaffPlusPlusDiscord.get();
        RegisteredServiceProvider<IStaffPlus> provider = Bukkit.getServicesManager().getRegistration(IStaffPlus.class);
        if (provider != null) {
            this.staffPlus = provider.getProvider();
        }
    }

    public void init() {
        if (staffPlusPlusDiscord.getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            discordListener = new DiscordStaffChatListener(staffPlusPlusDiscord, staffPlus);
            DiscordSRV.api.subscribe(discordListener);
        }
    }

    @Override
    public void teardown() {
        if (staffPlusPlusDiscord.getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            DiscordSRV.api.unsubscribe(discordListener);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleStaffChatEvent(StaffChatEvent event) {
        if (!syncEnabled) {
            return;
        }
        // Send to discord off the main thread (just like DiscordSRV does)
        staffPlusPlusDiscord.getServer().getScheduler().runTaskAsynchronously(staffPlusPlusDiscord, () ->
            DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), DiscordStaffChatListener.CHANNEL_PREFIX + event.getChannel(), false)
        );
    }

    @Override
    public boolean isEnabled() {
        return syncEnabled && staffPlus != null;
    }

    @Override
    public void validate() {
        boolean discordSRVEnabled = staffPlusPlusDiscord.getServer().getPluginManager().isPluginEnabled("DiscordSRV");
        if(isEnabled() && !discordSRVEnabled) {
            throw new RuntimeException("DiscordSRV plugin not enabled! Disable Staffchat sync or enable the DiscordSRV plugin.");
        }
    }
}
