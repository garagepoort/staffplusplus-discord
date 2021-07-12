package be.garagepoort.staffplusplus.discord.xray;

import be.garagepoort.mcioc.configuration.IConfigTransformer;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OresTransformer implements IConfigTransformer<List<String>, String> {
    @Override
    public List<String> mapConfig(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(";"));
    }
}
