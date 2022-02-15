package be.garagepoort.staffplusplus.discord.domain.ban;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordClientBuilder;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfig;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import net.shortninja.staffplusplus.ban.IBan;
import net.shortninja.staffplusplus.ban.appeals.BanAppealApprovedEvent;
import net.shortninja.staffplusplus.ban.appeals.BanAppealRejectedEvent;
import net.shortninja.staffplusplus.ban.appeals.BanAppealedEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class AppealListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.bans.appeals.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.bans.appeals.notifyCreate")
    private boolean notifyCreate;
    @ConfigProperty("StaffPlusPlusDiscord.bans.appeals.notifyApproved")
    private boolean notifyApproved;
    @ConfigProperty("StaffPlusPlusDiscord.bans.appeals.notifyRejected")
    private boolean notifyRejected;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public AppealListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleCreateAppeal(BanAppealedEvent event) {
        if (!notifyCreate) {
            return;
        }

        buildAppeal(event.getBan(), "bans/appeals/appeal-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealApproved(BanAppealApprovedEvent event) {
        if (!notifyApproved) {
            return;
        }

        buildAppeal(event.getBan(), "bans/appeals/appeal-approved");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealRejected(BanAppealRejectedEvent event) {
        if (!notifyRejected) {
            return;
        }

        buildAppeal(event.getBan(), "bans/appeals/appeal-rejected");
    }

    private void buildAppeal(IBan ban, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(ban.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("ban", ban);
        jc.set("appeal", ban.getAppeal().get());
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyCreate;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No ban appeals webhookUrl provided in the configuration.");
        }
    }
}
