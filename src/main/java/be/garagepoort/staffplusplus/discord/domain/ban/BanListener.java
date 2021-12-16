package be.garagepoort.staffplusplus.discord.domain.ban;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordClientBuilder;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.JavaUtils;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfig;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import net.shortninja.staffplusplus.ban.BanEvent;
import net.shortninja.staffplusplus.ban.BanExtensionEvent;
import net.shortninja.staffplusplus.ban.BanReductionEvent;
import net.shortninja.staffplusplus.ban.IBan;
import net.shortninja.staffplusplus.ban.UnbanEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class BanListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.bans.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
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
    private final DiscordClientBuilder discordClientBuilder;

    public BanListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
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
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
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
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    private void buildBan(IBan ban, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(ban.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("ban", ban);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyBan || notifyUnban;
    }

    @Override
    public void validate() {
        if(isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No bans webhookUrl provided in the configuration.");
        }
    }
}
