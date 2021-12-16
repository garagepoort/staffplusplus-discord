package be.garagepoort.staffplusplus.discord.domain.altdetect;

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
import net.shortninja.staffplusplus.altdetect.AltDetectEvent;
import net.shortninja.staffplusplus.altdetect.AltDetectTrustLevel;
import net.shortninja.staffplusplus.altdetect.IAltDetectResult;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class AltDetectionListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.altDetect.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.altDetect.enabledTrustLevels")
    @ConfigTransformer(AltDetectTrustLevelTransformer.class)
    private List<AltDetectTrustLevel> enabledTrustLevels = new ArrayList<>();

    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;
    private DiscordClient discordClient;

    public AltDetectionListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAltDetectionEvent(AltDetectEvent event) {
        IAltDetectResult altDetectResult = event.getAltDetectResult();
        if (enabledTrustLevels.contains(altDetectResult.getAltDetectTrustLevel())) {
            buildDetectionResult(event.getAltDetectResult(), "altdetects/detected");
        }
    }

    private void buildDetectionResult(IAltDetectResult detectionResult, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        JexlContext jc = new MapContext();
        jc.set("detectionResult", detectionResult);
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return enabledTrustLevels != null && !enabledTrustLevels.isEmpty();
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No altdetect webhookUrl provided in the configuration.");
        }
    }
}
