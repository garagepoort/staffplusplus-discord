package be.garagepoort.staffplusplus.discord.domain.reports;

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
import net.shortninja.staffplusplus.reports.AcceptReportEvent;
import net.shortninja.staffplusplus.reports.CreateReportEvent;
import net.shortninja.staffplusplus.reports.IReport;
import net.shortninja.staffplusplus.reports.RejectReportEvent;
import net.shortninja.staffplusplus.reports.ReopenReportEvent;
import net.shortninja.staffplusplus.reports.ResolveReportEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class ReportListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.reports.webhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig reportsWebhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.reports.playerReportsWebhookUrl")
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig playerReportsWebhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.reports.notifyOpen")
    private boolean notifyOpen;
    @ConfigProperty("StaffPlusPlusDiscord.reports.notifyReopen")
    private boolean notifyReopen;
    @ConfigProperty("StaffPlusPlusDiscord.reports.notifyAccept")
    private boolean notifyAccept;
    @ConfigProperty("StaffPlusPlusDiscord.reports.notifyReject")
    private boolean notifyReject;
    @ConfigProperty("StaffPlusPlusDiscord.reports.notifyResolve")
    private boolean notifyResolve;

    private DiscordClient reportDiscordClient;
    private DiscordClient playerReportDiscordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public ReportListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        reportDiscordClient = discordClientBuilder.buildClient(reportsWebhookUrl.getHost());
        if (playerReportsWebhookUrl != null) {
            playerReportDiscordClient = discordClientBuilder.buildClient(playerReportsWebhookUrl.getHost());
        } else {
            playerReportDiscordClient = reportDiscordClient;
            playerReportsWebhookUrl = reportsWebhookUrl;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleCreateReport(CreateReportEvent event) {
        if (!notifyOpen) {
            return;
        }

        buildReport(event.getReport(), "reports/report-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleReopenReport(ReopenReportEvent event) {
        if (!notifyReopen) {
            return;
        }

        buildReport(event.getReport(), "reports/report-reopened");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAcceptReport(AcceptReportEvent event) {
        if (!notifyAccept) {
            return;
        }

        buildReport(event.getReport(), "reports/report-accepted");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleRejectReport(RejectReportEvent event) {
        if (!notifyReject) {
            return;
        }

        buildReport(event.getReport(), "reports/report-rejected");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleResolveReport(ResolveReportEvent event) {
        if (!notifyResolve) {
            return;
        }

        buildReport(event.getReport(), "reports/report-resolved");
    }

    public void buildReport(IReport report, String key) {
        String createReportTemplate = replaceReportCreatedTemplate(report, templateRepository.getTemplate(key));
        DiscordClient discordClient = report.getCulpritUuid() == null ? this.reportDiscordClient : playerReportDiscordClient;
        WebhookConfig webhookConfig = report.getCulpritUuid() == null ? this.reportsWebhookUrl : playerReportsWebhookUrl;
        DiscordUtil.sendEvent(discordClient, webhookConfig, createReportTemplate);
    }

    private String replaceReportCreatedTemplate(IReport report, String createReportTemplate) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(report.getCreationDate().toInstant(), ZoneOffset.UTC);

        JexlContext jc = new MapContext();
        jc.set("report", report);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return JexlTemplateParser.parse(createReportTemplate, jc);
    }

    public boolean isEnabled() {
        return notifyOpen || notifyReopen || notifyAccept || notifyReject || notifyResolve;
    }

    @Override
    public void validate() {
        if (isEnabled() && reportsWebhookUrl == null) {
            throw new RuntimeException("No reports webhookUrl provided in the configuration.");
        }
    }
}
