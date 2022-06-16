package be.garagepoort.staffplusplus.discord.domain.staffmode;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordClientBuilder;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfig;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.templates.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.templates.TemplateRepository;
import net.shortninja.staffplusplus.staffmode.EnterStaffModeEvent;
import net.shortninja.staffplusplus.staffmode.ExitStaffModeEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class StaffModeListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.staffmode.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-enter")
    private boolean notifyEnter;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-exit")
    private boolean notifyExit;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public StaffModeListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder)  {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handEnterStaffMode(EnterStaffModeEvent event) {
        if(!notifyEnter) {
            return;
        }
        buildEnterStaffModeResult(event, "staffmode/enter-staffmode");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handEnterStaffMode(ExitStaffModeEvent event) {
        if(!notifyExit) {
            return;
        }
        buildEnterStaffModeResult(event, "staffmode/exit-staffmode");
    }

    private void buildEnterStaffModeResult(Event event, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        JexlContext jc = new MapContext();
        jc.set("staffmodeEvent", event);
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return notifyEnter || notifyExit;
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            StaffPlusPlusDiscord.get().getLogger().warning("No staffmode webhookUrl provided in the configuration.");
        }
    }
}
