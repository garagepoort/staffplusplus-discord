package be.garagepoort.staffplusplus.discord.domain.chat;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
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
import net.shortninja.staffplusplus.chat.PhrasesDetectedEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class ChatListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.chat.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.chat.phrase-detection")
    private boolean notifyPhraseDetection;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public ChatListener(TemplateRepository templateRepository)  {
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
    public void handlePhraseDetectedEvent(PhrasesDetectedEvent event) {
        if (!notifyPhraseDetection) {
            return;
        }

        buildPhraseDetected(event, "chat/chat-phrase-detected");
    }

    private void buildPhraseDetected(PhrasesDetectedEvent detectedEvent, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JexlContext jc = new MapContext();
        jc.set("detectedEvent", detectedEvent);
        jc.set("timestamp", time);
        jc.set("detectedPhrases", String.join(" | ", detectedEvent.getDetectedPhrases()));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return notifyPhraseDetection;
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No chat webhookUrl provided in the configuration.");
        }
    }
}
