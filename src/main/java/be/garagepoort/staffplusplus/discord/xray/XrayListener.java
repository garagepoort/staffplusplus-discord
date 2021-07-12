package be.garagepoort.staffplusplus.discord.xray;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import be.garagepoort.mcioc.configuration.ConfigProperty;
import be.garagepoort.mcioc.configuration.ConfigTransformer;
import be.garagepoort.staffplusplus.discord.common.StaffPlusPlusListener;
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
import net.shortninja.staffplusplus.xray.XrayEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@IocBean
@IocMultiProvider(StaffPlusPlusListener.class)
public class XrayListener implements StaffPlusPlusListener {

    @ConfigProperty("StaffPlusPlusDiscord.xray.webhookUrl")
    private String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.xray.enabledOres")
    @ConfigTransformer(OresTransformer.class)
    private List<String> enabledOres = new ArrayList<>();

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;

    public XrayListener(TemplateRepository templateRepository) {
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
    public void handlePhraseDetectedEvent(XrayEvent event) {
        Material material = event.getType();
        if (enabledOres.contains(material.name())) {
            buildXray(event, "xray/xray");
        }
    }

    private void buildXray(XrayEvent event, String templateFile) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JexlContext jc = new MapContext();
        jc.set("xrayEvent", event);
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, template);
    }

    public boolean isEnabled() {
        return enabledOres != null && !enabledOres.isEmpty();
    }

    @Override
    public void validate() {
        if (isEnabled() && StringUtils.isBlank(webhookUrl)) {
            throw new RuntimeException("No xray webhookUrl provided in the configuration.");
        }
    }
}
