package be.garagepoort.staffplusplus.discord.domain.xray;

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
import net.shortninja.staffplusplus.xray.XrayEvent;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
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
    @ConfigTransformer(WebhookConfigTransformer.class)
    private WebhookConfig webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.xray.enabledOres")
    @ConfigTransformer(OresTransformer.class)
    private List<String> enabledOres = new ArrayList<>();

    private DiscordClient discordClient;
    private final TemplateRepository templateRepository;
    private final DiscordClientBuilder discordClientBuilder;

    public XrayListener(TemplateRepository templateRepository, DiscordClientBuilder discordClientBuilder) {
        this.templateRepository = templateRepository;
        this.discordClientBuilder = discordClientBuilder;
    }

    public void init() {
        discordClient = discordClientBuilder.buildClient(webhookUrl.getHost());
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

        List<String> enchantments = new ArrayList<>();
        event.getPickaxe().getEnchantments()
            .forEach((k, v) -> enchantments.add(k.getName() + " " + v));
        JexlContext jc = new MapContext();
        jc.set("xrayEvent", event);
        jc.set("enchantments", String.join("\\\\n", enchantments));
        jc.set("timestamp", time);
        String template = JexlTemplateParser.parse(templateRepository.getTemplate(templateFile), jc);
        DiscordUtil.sendEvent(discordClient, webhookUrl, template);
    }

    public boolean isEnabled() {
        return enabledOres != null && !enabledOres.isEmpty();
    }

    @Override
    public void validate() {
        if (isEnabled() && webhookUrl == null) {
            StaffPlusPlusDiscord.get().getLogger().warning("No xray webhookUrl provided in the configuration.");
        }
    }
}
