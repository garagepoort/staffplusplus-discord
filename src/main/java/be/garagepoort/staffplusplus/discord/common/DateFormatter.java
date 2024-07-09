package be.garagepoort.staffplusplus.discord.common;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.configuration.ConfigProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@IocBean
public class DateFormatter {

    private DateTimeFormatter dateTimeFormatter;

    public DateFormatter(@ConfigProperty("StaffPlusPlusDiscord.timestamp-format") String timestampFormat) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(timestampFormat);
    }

    public String format(LocalDateTime localDateTime) {
        return dateTimeFormatter.format(localDateTime);
    }
}
