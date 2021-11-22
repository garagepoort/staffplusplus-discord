package be.garagepoort.staffplusplus.discord.domain.ban;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.staffplusplus.discord.common.JavaUtils;
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
import net.shortninja.staffplusplus.ban.BanEvent;
import net.shortninja.staffplusplus.ban.BanExtensionEvent;
import net.shortninja.staffplusplus.ban.BanReductionEvent;
import net.shortninja.staffplusplus.ban.IBan;
import net.shortninja.staffplusplus.ban.UnbanEvent;
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
public class BanListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.bans.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.bans.ban")
    private boolean notifyBan;
    @ConfigProperty("StaffPlusPlusDiscord.bans.unban")
    private boolean notifyUnban;
    @ConfigProperty("StaffPlusPlusDiscord.bans.extension")
    private boolean notifyExtension;
    @ConfigProperty("StaffPlusPlusDiscord.bans.reduction")
    private boolean notifyReduction;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public BanListener(TemplateRepository templateRepository)  {
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
    public void handleBanEvent(BanEvent event) {
        if (!notifyBan) {
            return;
        }

        buildBan(event.getBan(), "bans/banned");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleUnbanEvent(UnbanEvent event) {
        if (!notifyUnban) {
            return;
        }

        buildBan(event.getBan(), "bans/unbanned");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleExtendBanEvent(BanExtensionEvent event) {
        if (!notifyExtension) {
            return;
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(event.getBan().getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("ban", event.getBan());
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        jc.set("extensionExecutor", event.getExecutor());
        jc.set("extensionDuration", JavaUtils.toHumanReadableDuration(event.getExtensionDuration()));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate("bans/extension"), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleReduceBanEvent(BanReductionEvent event) {
        if (!notifyReduction) {
            return;
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(event.getBan().getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("ban", event.getBan());
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        jc.set("reductionExecutor", event.getExecutor());
        jc.set("reductionDuration", JavaUtils.toHumanReadableDuration(event.getReductionDuration()));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate("bans/reduction"), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    private void buildBan(IBan ban, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(ban.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("ban", ban);
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
            throw new RuntimeException("No bans webhookUrl provided in the configuration.");
        }
    }
}
