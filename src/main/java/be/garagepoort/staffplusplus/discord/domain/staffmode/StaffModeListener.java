package be.garagepoort.staffplusplus.discord.domain.staffmode;

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
import net.shortninja.staffplusplus.staffmode.EnterStaffModeEvent;
import net.shortninja.staffplusplus.staffmode.ExitStaffModeEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class StaffModeListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.staffmode.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-enter")
    private boolean notifyEnter;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-exit")
    private boolean notifyExit;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public StaffModeListener(TemplateRepository templateRepository)  {
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
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return notifyEnter || notifyExit;
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No staffmode webhookUrl provided in the configuration.");
        }
    }
}
