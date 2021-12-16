package be.garagepoort.staffplusplus.discord.domain.chat;

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
import net.shortninja.staffplusplus.chat.PhrasesDetectedEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class ChatListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.chat.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.chat.phrase-detection")
    private boolean notifyPhraseDetection;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public ChatListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
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
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyPhraseDetection;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No chat webhookUrl provided in the configuration.");
        }
    }
}
