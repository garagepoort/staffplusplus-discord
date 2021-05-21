package be.garagepoort.staffplusplus.discord.staffchat;

import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import net.shortninja.staffplusplus.IStaffPlus;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DiscordStaffChatListener {
    private final StaffPlusPlusDiscord plugin;
    private final IStaffPlus staffPlus;
    public static final String CHANNEL_PREFIX = "staffplusplus-";

    public DiscordStaffChatListener(StaffPlusPlusDiscord plugin, IStaffPlus staffPlus) {
        this.plugin = plugin;
        this.staffPlus = staffPlus;
    }

    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePostProcessEvent event) {
        Optional<String> channelName = getChannelName(event.getChannel().getId());
        if(!channelName.isPresent()) {
            return;
        }

        if (isStaffChatChannel(channelName.get(), event.getChannel().getId())) {
            event.setCancelled(true); // Cancel this message from getting sent to global chat.

            // Handle this on the main thread next tick.
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    submitMessageFromDiscord(channelName.get().replace(CHANNEL_PREFIX, ""), event.getAuthor(), event.getProcessedMessage())
            );
        }
    }

    private boolean isStaffChatChannel(String channelName, String id) {
        if (channelName != null && channelName.startsWith(CHANNEL_PREFIX)) {
            TextChannel destinationTextChannelForGameChannelName = DiscordSRV.getPlugin().getJda().getTextChannelById(id);
            if (destinationTextChannelForGameChannelName == null) {
                throw new RuntimeException("DiscordSRV not setup correctly. No channel configured with name [" + channelName + "]");
            }
            return true;
        }
        return false;
    }

    private Optional<String> getChannelName(String id) {
        Map<String, String> channels = DiscordSRV.getPlugin().getChannels();
        String channelName = null;
        for (Map.Entry<String, String> entry : channels.entrySet()) {
            if (entry.getValue().equals(id)) {
                return Optional.ofNullable(entry.getKey());
            }
        }
        return Optional.ofNullable(channelName);
    }

    public void submitMessageFromDiscord(String channel, User author, String message) {
        Objects.requireNonNull(author, "channel");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        staffPlus.getStaffChatService().sendMessage(channel, message);
    }
}
