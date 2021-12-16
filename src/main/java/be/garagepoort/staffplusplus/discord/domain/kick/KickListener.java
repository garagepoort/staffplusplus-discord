package be.garagepoort.staffplusplus.discord.domain.kick;

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
import net.shortninja.staffplusplus.kick.IKick;
import net.shortninja.staffplusplus.kick.KickEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class KickListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.kicks.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.kicks.kick")
    private boolean notifyKick;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public KickListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
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
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyKick;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No kick webhookUrl provided in the configuration.");
        }
    }
}
