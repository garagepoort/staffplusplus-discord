package be.garagepoort.staffplusplus.discord.mute;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.TemplateRepository;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.shortninja.staffplusplus.mute.IMute;
import net.shortninja.staffplusplus.mute.MuteEvent;
import net.shortninja.staffplusplus.mute.UnmuteEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class MuteListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.mutes.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.mute")
    private boolean notifyMute;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.unmute")
    private boolean notifyUnmute;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public MuteListener(TemplateRepository templateRepository) {
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
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return notifyMute || notifyUnmute;
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No mute webhookUrl provided in the configuration.");
        }
    }
}
