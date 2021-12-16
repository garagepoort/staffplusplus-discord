package be.garagepoort.staffplusplus.discord.common.config;

import be.garagepoort.mcioc.configuration.IConfigTransformer;
import org.apache.commons.lang.StringUtils;

public class WebhookConfigTransformer implements IConfigTransformer<WebhookConfig, String> {

    @Override
    public WebhookConfig mapConfig(String s) {
        if(StringUtils.isBlank(s)) {
            return null;
        }
        int index = s.lastIndexOf("/");
        String[] parts =  {s.substring(0, index+1), s.substring(index+1)};
        return new WebhookConfig(parts[0], parts[1]);
    }
}
