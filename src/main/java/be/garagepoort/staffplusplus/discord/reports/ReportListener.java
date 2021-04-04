package be.garagepoort.staffplusplus.discord.reports;

import be.garagepoort.staffplusplus.discord.StaffPlusPlusListener;
import be.garagepoort.staffplusplus.discord.api.DiscordClient;
import be.garagepoort.staffplusplus.discord.api.DiscordUtil;
import be.garagepoort.staffplusplus.discord.common.JexlTemplateParser;
import be.garagepoort.staffplusplus.discord.common.TemplateRepository;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.shortninja.staffplusplus.reports.*;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ReportListener implements StaffPlusPlusListener {

    private DiscordClient reportDiscordClient;
    private DiscordClient playerReportDiscordClient;
    private FileConfiguration config;
    private final TemplateRepository templateRepository;

    public ReportListener(FileConfiguration config, TemplateRepository templateRepository) {
        this.config = config;
        this.templateRepository = templateRepository;
    }

    public void init() {
        String reportWebhookUrl = config.getString("StaffPlusPlusDiscord.reports.webhookUrl");
        String playerReportWebhookUrl = config.getString("StaffPlusPlusDiscord.reports.playerReportsWebhookUrl", null);

        reportDiscordClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(DiscordClient.class))
                .logLevel(Logger.Level.FULL)
                .target(DiscordClient.class, reportWebhookUrl);

        if (StringUtils.isNotEmpty(playerReportWebhookUrl)) {
            playerReportDiscordClient = Feign.builder()
                    .client(new OkHttpClient())
                    .encoder(new GsonEncoder())
                    .decoder(new GsonDecoder())
                    .logger(new Slf4jLogger(DiscordClient.class))
                    .logLevel(Logger.Level.FULL)
                    .target(DiscordClient.class, playerReportWebhookUrl);
        } else {
            playerReportDiscordClient = reportDiscordClient;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleCreateReport(CreateReportEvent event) {
        if (!config.getBoolean("StaffPlusPlusDiscord.reports.notifyOpen")) {
            return;
        }

        buildReport(event.getReport(), "reports/report-created");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleReopenReport(ReopenReportEvent event) {
        if (!config.getBoolean("StaffPlusPlusDiscord.reports.notifyReopen")) {
            return;
        }

        buildReport(event.getReport(), "reports/report-reopened");
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void handleAcceptReport(AcceptReportEvent event) {
        if (!config.getBoolean("StaffPlusPlusDiscord.reports.notifyAccept")) {
            return;
        }

        buildReport(event.getReport(), "reports/report-accepted");
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void handleRejectReport(RejectReportEvent event) {
        if (!config.getBoolean("StaffPlusPlusDiscord.reports.notifyReject")) {
            return;
        }

        buildReport(event.getReport(), "reports/report-rejected");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleResolveReport(ResolveReportEvent event) {
        if (!config.getBoolean("StaffPlusPlusDiscord.reports.notifyResolve")) {
            return;
        }

        buildReport(event.getReport(), "reports/report-resolved");
    }

    public void buildReport(IReport report, String key) {
        String createReportTemplate = replaceReportCreatedTemplate(report, templateRepository.getTemplate(key));
        DiscordClient discordClient = report.getCulpritUuid() == null ? this.reportDiscordClient : playerReportDiscordClient;
        DiscordUtil.sendEvent(discordClient, createReportTemplate);
    }

    private String replaceReportCreatedTemplate(IReport report, String createReportTemplate) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(report.getCreationDate().toInstant(), ZoneOffset.UTC);

        JexlContext jc = new MapContext();
        jc.set("report", report);
        jc.set("timestamp", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return JexlTemplateParser.parse(createReportTemplate, jc);
    }

    public boolean isEnabled() {
        return config.getBoolean("StaffPlusPlusDiscord.reports.notifyOpen") ||
                config.getBoolean("StaffPlusPlusDiscord.reports.notifyReopen") ||
                config.getBoolean("StaffPlusPlusDiscord.reports.notifyAccept") ||
                config.getBoolean("StaffPlusPlusDiscord.reports.notifyReject") ||
                config.getBoolean("StaffPlusPlusDiscord.reports.notifyResolve");
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(config.getString("StaffPlusPlusDiscord.reports.webhookUrl"));
    }
}
