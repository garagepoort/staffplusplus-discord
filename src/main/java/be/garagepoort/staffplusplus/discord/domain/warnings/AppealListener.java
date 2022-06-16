package be.garagepoort.staffplusplus.discord.domain.warnings;

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
import net.shortninja.staffplusplus.warnings.IWarning;
import net.shortninja.staffplusplus.warnings.WarningAppealApprovedEvent;
import net.shortninja.staffplusplus.warnings.WarningAppealRejectedEvent;
import net.shortninja.staffplusplus.warnings.WarningAppealedEvent;
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

    @ConfigProperty("StaffPlusPlusDiscord.warnings.appeals.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.appeals.notifyCreate")
    private boolean notifyCreate;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.appeals.notifyApproved")
    private boolean notifyApproved;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.appeals.notifyRejected")
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
    public void handleCreateAppeal(WarningAppealedEvent event) {
        if (!notifyCreate) {
            return;
        }

        buildAppeal(event.getWarning(), "appeals/appeal-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealApproved(WarningAppealApprovedEvent event) {
        if (!notifyApproved) {
            return;
        }

        buildAppeal(event.getWarning(), "appeals/appeal-approved");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealRejected(WarningAppealRejectedEvent event) {
        if (!notifyRejected) {
            return;
        }

        buildAppeal(event.getWarning(), "appeals/appeal-rejected");
    }

    private void buildAppeal(IWarning warning, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(warning.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("warning", warning);
        jc.set("appeal", warning.getAppeal().get());
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
            StaffPlusPlusDiscord.get().getLogger().warning("No appeals webhookUrl provided in the configuration.");
        }
    }
}
