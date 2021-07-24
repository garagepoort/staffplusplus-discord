package be.garagepoort.staffplusplus.discord.domain.investigations;

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
import net.shortninja.staffplusplus.investigate.IInvestigation;
import net.shortninja.staffplusplus.investigate.IInvestigationNote;
import net.shortninja.staffplusplus.investigate.InvestigationConcludedEvent;
import net.shortninja.staffplusplus.investigate.InvestigationNoteCreatedEvent;
import net.shortninja.staffplusplus.investigate.InvestigationStartedEvent;
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
public class InvestigationListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.investigations.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.investigations.notify-start")
    private boolean notifyStart;
    @ConfigProperty("StaffPlusPlusDiscord.investigations.notify-conclude")
    private boolean notifyConclude;
    @ConfigProperty("StaffPlusPlusDiscord.investigations.notify-note-created")
    private boolean notifyNoteCreated;

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public InvestigationListener(TemplateRepository templateRepository) {
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
    public void handleStartEvent(InvestigationStartedEvent event) {
        if (!notifyStart) {
            return;
        }

        IInvestigation investigation = event.getInvestigation();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(investigation.getCreationDate().toInstant(), ZoneOffset.UTC);

        sendDiscordEvent(investigation, null, localDateTime, "investigations/started");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleConcludeEvent(InvestigationConcludedEvent event) {
        if (!notifyConclude) {
            return;
        }

        IInvestigation investigation = event.getInvestigation();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(investigation.getConclusionDate().get().toInstant(), ZoneOffset.UTC);

        sendDiscordEvent(investigation, null, localDateTime, "investigations/concluded");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleNoteCreated(InvestigationNoteCreatedEvent event) {
        if (!notifyNoteCreated) {
            return;
        }

        IInvestigation investigation = event.getInvestigation();
        IInvestigationNote investigationNote = event.getInvestigationNote();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(investigationNote.getCreationDate().toInstant(), ZoneOffset.UTC);

        sendDiscordEvent(investigation, investigationNote, localDateTime, "investigations/note-created");
    }

    public boolean isEnabled() {
        return notifyStart || notifyConclude || notifyNoteCreated;
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No investigations webhookUrl provided in the configuration.");
        }
    }

    private void sendDiscordEvent(IInvestigation investigation, IInvestigationNote investigationNote, LocalDateTime localDateTime, String s) {
        JexlContext jc = new MapContext();
        jc.set("investigation", investigation);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (investigationNote != null) {
            jc.set("note", investigationNote);
        }
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(s), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }
}
