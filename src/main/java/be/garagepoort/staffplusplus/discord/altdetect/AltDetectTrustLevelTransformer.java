package be.garagepoort.staffplusplus.discord.altdetect;

import be.garagepoort.mcioc.configuration.IConfigTransformer;
import net.shortninja.staffplusplus.altdetect.AltDetectTrustLevel;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AltDetectTrustLevelTransformer implements IConfigTransformer<List<AltDetectTrustLevel>, String> {
    @Override
    public List<AltDetectTrustLevel> mapConfig(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(";"))
                .map(AltDetectTrustLevel::valueOf)
                .collect(Collectors.toList());
    }
}
