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
import net.shortninja.staffplusplus.mute.appeals.MuteAppealApprovedEvent;
import net.shortninja.staffplusplus.mute.appeals.MuteAppealRejectedEvent;
import net.shortninja.staffplusplus.mute.appeals.MuteAppealedEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class AppealListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.mutes.appeals.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.appeals.notifyCreate")
    private boolean notifyCreate;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.appeals.notifyApproved")
    private boolean notifyApproved;
    @ConfigProperty("StaffPlusPlusDiscord.mutes.appeals.notifyRejected")
    private boolean notifyRejected;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public AppealListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleCreateAppeal(MuteAppealedEvent event) {
        if (!notifyCreate) {
            return;
        }

        buildAppeal(event.getMute(), "mutes/appeals/appeal-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealApproved(MuteAppealApprovedEvent event) {
        if (!notifyApproved) {
            return;
        }

        buildAppeal(event.getMute(), "mutes/appeals/appeal-approved");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAppealRejected(MuteAppealRejectedEvent event) {
        if (!notifyRejected) {
            return;
        }

        buildAppeal(event.getMute(), "mutes/appeals/appeal-rejected");
    }

    private void buildAppeal(IMute mute, String templateFile) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(mute.getCreationDate().toInstant(), ZoneOffset.UTC);
        JexlContext jc = new MapContext();
        jc.set("mute", mute);
        jc.set("appeal", mute.getAppeal().get());
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyCreate;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            throw new RuntimeException("No mute appeals webhookUrl provided in the configuration.");
        }
    }
}
