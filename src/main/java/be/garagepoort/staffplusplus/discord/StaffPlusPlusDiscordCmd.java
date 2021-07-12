package be.garagepoort.staffplusplus.discord;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocCommandHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@IocBean
@IocCommandHandler("staffplusplusdiscord")
public class StaffPlusPlusDiscordCmd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && commandSender.hasPermission("staffplusplusdiscord.reload")) {
            StaffPlusPlusDiscord.get().reload();
            commandSender.sendMessage("[S++ Discord] Configuration reloaded");
        } else {
            commandSender.sendMessage("[S++ Discord] Running version " + StaffPlusPlusDiscord.get().getDescription().getVersion());
        }
        return true;
    }
}
