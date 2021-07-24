package be.garagepoort.staffplusplus.discord.common.templates;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.staffplusplus.discord.StaffPlusPlusConfiguration;
import be.garagepoort.staffplusplus.discord.common.Utils;

import java.util.HashMap;
import java.util.Map;

import static be.garagepoort.staffplusplus.discord.common.templates.TemplateResourceUtil.getFullTemplatePath;
import static java.util.Arrays.stream;

@IocBean
public class TemplateRepository {

    private final Map<String, String> templates = new HashMap<>();

    private static final TemplateFile[] templateFiles = {
        new TemplateFile("reports", "report-created"),
        new TemplateFile("reports", "report-accepted"),
        new TemplateFile("reports", "report-resolved"),
        new TemplateFile("reports", "report-rejected"),
        new TemplateFile("reports", "report-reopened"),
        new TemplateFile("warnings", "warning-created"),
        new TemplateFile("warnings", "threshold-reached"),
        new TemplateFile("appeals", "appeal-created"),
        new TemplateFile("appeals", "appeal-approved"),
        new TemplateFile("appeals", "appeal-rejected"),
        new TemplateFile("bans", "banned"),
        new TemplateFile("bans", "unbanned"),
        new TemplateFile("bans", "ip-banned"),
        new TemplateFile("bans", "ip-unbanned"),
        new TemplateFile("mutes", "muted"),
        new TemplateFile("mutes", "unmuted"),
        new TemplateFile("kicks", "kicked"),
        new TemplateFile("investigations", "started"),
        new TemplateFile("investigations", "concluded"),
        new TemplateFile("investigations", "note-created"),
        new TemplateFile("altdetects", "detected"),
        new TemplateFile("staffmode", "enter-staffmode"),
        new TemplateFile("staffmode", "exit-staffmode"),
        new TemplateFile("chat", "chat-phrase-detected"),
        new TemplateFile("xray", "xray")
    };

    public TemplateRepository(StaffPlusPlusConfiguration staffPlusPlusConfiguration) {
        boolean replaceTemplates = staffPlusPlusConfiguration.updateTemplates;
        String templatePack = staffPlusPlusConfiguration.templatePack;

        stream(templateFiles)
            .forEach(templateFile -> TemplateResourceUtil.saveTemplate(templatePack, templateFile.getFilePath(), replaceTemplates));

        for (TemplateFile templateFile : templateFiles) {
            templates.put(templateFile.getId(), Utils.readTemplate(getFullTemplatePath(templatePack, templateFile)));
        }
    }

    public String getTemplate(String key) {
        if (!templates.containsKey(key)) {
            throw new RuntimeException("No template found with key: [" + key + "]");
        }
        return templates.get(key);
    }
}