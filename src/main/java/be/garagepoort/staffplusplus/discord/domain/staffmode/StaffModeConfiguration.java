package be.garagepoort.staffplusplus.discord.domain.staffmode;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.configuration.ConfigProperty;

@IocBean
public class StaffModeConfiguration {

    @ConfigProperty("StaffPlusPlusDiscord.staffmode.webhookUrl")
    public String webhookUrl;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-enter")
    public boolean notifyEnter;
    @ConfigProperty("StaffPlusPlusDiscord.staffmode.notify-exit")
    public boolean notifyExit;

}
