package be.garagepoort.staffplusplus.discord.domain.blacklist;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordClientBuilder;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfig;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import net.shortninja.staffplusplus.blacklist.BlacklistCensoredEvent;
import net.shortninja.staffplusplus.blacklist.BlacklistType;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class BlacklistListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.blacklist.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.blacklist.notifyChatCensored")
    private boolean notifyChat;
    @ConfigProperty("StaffPlusPlusDiscord.blacklist.notifySignsCensored")
    private boolean notifySigns;
    @ConfigProperty("StaffPlusPlusDiscord.blacklist.notifyAnvilCensored")
    private boolean notifyAnvil;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public BlacklistListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleBlacklistEvent(BlacklistCensoredEvent event) {
        if (notifyAnvil && event.getBlacklistType() == BlacklistType.ANVIL) {
            sendCensoredNotification(event, "blacklist/anvil");
        }
        if (notifyChat && event.getBlacklistType() == BlacklistType.CHAT) {
            sendCensoredNotification(event, "blacklist/chat");
        }
        if (notifySigns && event.getBlacklistType() == BlacklistType.SIGN) {
            sendCensoredNotification(event, "blacklist/signs");
        }

    }

    private void sendCensoredNotification(BlacklistCensoredEvent censoredEvent, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JexlContext jc = new MapContext();
        jc.set("censoredEvent", censoredEvent);
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyAnvil || notifyChat || notifySigns;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            StaffPlusPlusDiscord.get().getLogger().warning("No webhookUrl provided for Blacklist");
        }
    }
}
