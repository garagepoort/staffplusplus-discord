package be.garagepoort.staffplusplus.discord;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.configuration.ConfigProperty;

@IocBean
public class StaffPlusPlusConfiguration {

    @ConfigProperty("StaffPlusPlusDiscord.templatePack")
    public String templatePack = "default";

}
