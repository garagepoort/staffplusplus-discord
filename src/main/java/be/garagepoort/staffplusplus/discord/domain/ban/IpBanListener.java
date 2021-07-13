package be.garagepoort.staffplusplus.discord.domain.ban;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.shortninja.staffplusplus.ban.*;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.*;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class IpBanListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.ipbans.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.ipbans.ban")
    private boolean notifyBan;
    @ConfigProperty("StaffPlusPlusDiscord.ipbans.unban")
    private boolean notifyUnban;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public IpBanListener(TemplateRepository templateRepository)  {
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
    public void handleBanEvent(IpBanEvent event) {
        if (!notifyBan) {
            return;
        }

        buildBan(event.getBan(), "bans/ip-banned");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleUnbanEvent(IpUnbanEvent event) {
        if (!notifyUnban) {
            return;
        }

        buildBan(event.getBan(), "bans/ip-unbanned");
    }

    private void buildBan(IIpBan ban, String templateFile) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ban.getCreationDate()), ZoneId.systemDefault());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);

        JexlContext jc = new MapContext();
        jc.set("ipban", ban);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return notifyBan || notifyUnban;
    }

    @Override
    public void validate() {
        if(isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No ipbans webhookUrl provided in the configuration.");
        }
    }
}
