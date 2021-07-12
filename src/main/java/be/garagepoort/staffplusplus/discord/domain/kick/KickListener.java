package be.garagepoort.staffplusplus.discord.domain.kick;

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
import net.shortninja.staffplusplus.kick.IKick;
import net.shortninja.staffplusplus.kick.KickEvent;
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
public class KickListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.kicks.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.kicks.kick")
    private boolean notifyKick;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public KickListener(TemplateRepository templateRepository) {
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
    public void handleKickEvent(KickEvent event) {
        if (!notifyKick) {
            return;
        }

        buildKick(event.getKick(), "kicks/kicked");
    }

    private void buildKick(IKick kick, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(kick.getCreationDate().toInstant(), ZoneOffset.UTC);
        String time = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JexlContext jc = new MapContext();
        jc.set("kick", kick);
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return notifyKick;
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No kick webhookUrl provided in the configuration.");
        }
    }
}
