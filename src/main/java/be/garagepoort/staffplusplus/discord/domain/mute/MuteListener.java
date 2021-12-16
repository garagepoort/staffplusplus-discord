package be.garagepoort.staffplusplus.discord.domain.mute;

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
import net.shortninja.staffplusplus.mute.IMute;
import net.shortninja.staffplusplus.mute.MuteEvent;
import net.shortninja.staffplusplus.mute.UnmuteEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class MuteListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.mutes.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.mute")
    private boolean notifyMute;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.unmute")
    private boolean notifyUnmute;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public MuteListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleMuteEvent(MuteEvent event) {
        if (!notifyMute) {
            return;
        }

        IMute mute = event.getMute();
        buildMute(mute, "mutes/muted");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleUnmuteEvent(UnmuteEvent event) {
        if (!notifyUnmute) {
            return;
        }

        buildMute(event.getMute(), "mutes/unmuted");
    }

    private void buildMute(IMute mute, String templateFile) {

        LocalDateTime localDateTime = LocalDateTime.ofInstant(mute.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("mute", mute);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyMute || notifyUnmute;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No mute webhookUrl provided in the configuration.");
        }
    }
}
