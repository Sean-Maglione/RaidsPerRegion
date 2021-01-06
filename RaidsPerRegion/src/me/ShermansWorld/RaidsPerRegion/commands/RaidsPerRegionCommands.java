package me.ShermansWorld.RaidsPerRegion.commands;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.ShermansWorld.RaidsPerRegion.Main;


public class RaidsPerRegionCommands implements CommandExecutor {
	
	private Main plugin;

	public RaidsPerRegionCommands(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("raidsperregion").setExecutor((CommandExecutor) this); // command to run in chat
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender; // Convert sender into player
		World w = p.getWorld(); // Get world
		
		if (args.length == 0) {
			p.sendMessage("Invalid arguments");
			p.sendMessage("Cancel Raid: /raidsperregion cancel");
			p.sendMessage("Reload Config: /raidsperregion reload");
			return false;
		} else {
			if (args[0].equalsIgnoreCase("cancel")) {
				if (RaidCommands.region == null) { // if there is not a raid in progress
					p.sendMessage("There is not a raid in progress right now");
					return false;
				} else {
					p.sendMessage("Canceled raid on region " + RaidCommands.region.getId());
					Main.cancelledRaid = true;
					return false;
				}
			} else if (args[0].equalsIgnoreCase("reload")) {
				p.sendMessage("RaidsPerRegion config reloaded");
				return false;
			} else {
				p.sendMessage("Invalid arguments");
				p.sendMessage("Cancel Raid: /raidsperregion cancel");
				p.sendMessage("Reload Config: /raidsperregion reload");
				
				return false;
			}
		}
		
	}
	
}
