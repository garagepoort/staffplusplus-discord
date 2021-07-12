package be.garagepoort.staffplusplus.discord.domain.altdetect;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.shortninja.staffplusplus.altdetect.AltDetectEvent;
import net.shortninja.staffplusplus.altdetect.AltDetectTrustLevel;
import net.shortninja.staffplusplus.altdetect.IAltDetectResult;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
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
    private String webhookUrl = "";
    @ConfigProperty("StaffPlusPlusDiscord.altDetect.enabledTrustLevels")
    @ConfigTransformer(AltDetectTrustLevelTransformer.class)
    private List<AltDetectTrustLevel> enabledTrustLevels = new ArrayList<>();

    private final TemplateRepository templateRepository;
    private DiscordClient discordClient;

    public AltDetectionListener(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public void init() {
        discordClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(DiscordClient.class))
                .logLevel(Logger.Level.FULL)
                .target(DiscordClient.class, webhookUrl);
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
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return enabledTrustLevels != null && !enabledTrustLevels.isEmpty();
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No altdetect webhookUrl provided in the configuration.");
        }
    }
}
