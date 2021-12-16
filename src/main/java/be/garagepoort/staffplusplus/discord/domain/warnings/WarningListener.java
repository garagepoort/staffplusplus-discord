package be.garagepoort.staffplusplus.discord.domain.warnings;

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
import net.shortninja.staffplusplus.warnings.IWarning;
import net.shortninja.staffplusplus.warnings.WarningCreatedEvent;
import net.shortninja.staffplusplus.warnings.WarningThresholdReachedEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class WarningListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.warnings.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.notifyCleared")
    private boolean notifyCleared;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.notifyCreate")
    private boolean notifyCreate;
    @ConfigProperty("StaffPlusPlusDiscord.warnings.notifyThresholdReached")
    private boolean notifyThresholdReached;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public WarningListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleCreateWarning(WarningCreatedEvent event) {
        if (!notifyCreate) {
            return;
        }

        buildWarning(event.getWarning(), "warnings/warning-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleThresholdReachedWarning(WarningThresholdReachedEvent event) {
        if (!notifyThresholdReached) {
            return;
        }

        buildThreshold(event, "warnings/threshold-reached");
    }

    private void buildWarning(IWarning warning, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(warning.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("warning", warning);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    private void buildThreshold(WarningThresholdReachedEvent warning, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JexlContext jc = new MapContext();
        jc.set("threshold", warning);
        jc.set("commandsTriggered", String.join("\n", warning.getCommandsTriggered()));
        jc.set("timestamp", time);

        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyCreate || notifyCleared || notifyThresholdReached;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No warnings webhookUrl provided in the configuration.");
        }
    }
}
