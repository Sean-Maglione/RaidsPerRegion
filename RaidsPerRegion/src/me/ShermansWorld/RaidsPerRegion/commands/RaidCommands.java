package me.ShermansWorld.RaidsPerRegion.commands;
import me.ShermansWorld.RaidsPerRegion.Main;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.core.net.Priority;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import io.lumine.xikage.mythicmobs.mobs.MobManager;

public class RaidCommands implements CommandExecutor {
	
	private static Main plugin;
	
	private static boolean timeReached = false;
	private static int totalKills;
	private final int goal = 10; // the goal for the amount of kills, wins the raid
	private static boolean maxMobsReached = false;
	private int countdown = 120; // this is how long the raid will last in seconds
	private static List<String> mMMobNames = new ArrayList<>();
	private static List<Double> chances = new ArrayList<>();
	private static List<Integer> priorities = new ArrayList<>();
	private boolean runOnce = false;
	
	// public variables used in Listener Class
	public static List<Player> playersInRegion = new ArrayList<>();
	public static Map<String, Integer> raidKills = new HashMap<String, Integer>();
	public static ProtectedRegion region;
	public static int otherDeaths = 0;
	public static List<AbstractEntity> MmEntityList = new ArrayList<>();
	public static int mobsSpawned = 0;
	

	public RaidCommands(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("raid").setExecutor((CommandExecutor) this); // command to run in chat
	}

	// Player that sends command
	// Command it sends
	// Alias of the command which was used
	// args for Other values within command
	
	public void sendPacket(Player player, Object packet) {
	    try {
	        Object handle = player.getClass().getMethod("getHandle").invoke(player);
	        Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
	        playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public Class<?> getNMSClass(String name) {
	    try {
	        return Class.forName("net.minecraft.server."
	                + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
	    } catch (ClassNotFoundException e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public void send(Player player, String title, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
	    try {
	        Object chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class)
	                .invoke(null, "{\"text\": \"" + title + "\"}");
	        Constructor<?> titleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(
	                getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"),
	                int.class, int.class, int.class);
	        Object packet = titleConstructor.newInstance(
	                getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null), chatTitle,
	                fadeInTime, showTime, fadeOutTime);

	        Object chatsTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class)
	                .invoke(null, "{\"text\": \"" + subtitle + "\"}");
	        Constructor<?> timingTitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(
	                getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"),
	                int.class, int.class, int.class);
	        Object timingPacket = timingTitleConstructor.newInstance(
	                getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null), chatsTitle,
	                fadeInTime, showTime, fadeOutTime);

	        sendPacket(player, packet);
	        sendPacket(player, timingPacket);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public static void checkPlayersInRegion(ProtectedRegion region, Scoreboard board, Objective objective, MobManager mm, Score totalScore, List<String> mMMobNames, List<Double> chances, List<Integer> priorities, int maxMobsPerPlayer) {
		int[] id = {0};
		Random rand = new Random();
		id[0] = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				if (timeReached == true) {
					Bukkit.getServer().getScheduler().cancelTask(id[0]);
				} else {
					List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
					playersInRegion = new ArrayList<>();
					List<Location> onlinePlayerLocations = new ArrayList<>();
					List<Location> regionPlayerLocations = new ArrayList<>();
					int scoreCounter = 0;
					
					for (int i = 0; i < playerList.size(); i++) {
						onlinePlayerLocations.add(playerList.get(i).getLocation()); 
						if (region.contains(onlinePlayerLocations.get(i).getBlockX(),
								onlinePlayerLocations.get(i).getBlockY(), onlinePlayerLocations.get(i).getBlockZ())) {
							playersInRegion.add(playerList.get(i));
							regionPlayerLocations.add(playerList.get(i).getLocation()); 
						}
					}
					
					// Add up scores for all players in region
					for (int n = 0; n < playersInRegion.size(); n++) {
						
						// Make sure the player is mapped in raidKills or it will return a null error
						
						if (playersInRegion.get(n).getScoreboard() != board) {
							playersInRegion.get(n).setScoreboard(board);
						}
						
						if (raidKills.containsKey(playersInRegion.get(n).getName())) {
							Score score = objective.getScore(playersInRegion.get(n).getName());
							score.setScore(raidKills.get(playersInRegion.get(n).getName()));
							scoreCounter += score.getScore();
						}
						
					}
					
					// Spawn mobs for all players in region
					spawnMobs(rand, regionPlayerLocations, scoreCounter, totalScore, mm, mMMobNames, chances, priorities, maxMobsPerPlayer);
				}
			}
		}, 0L, 20L);
	}
	
	private void getMobsFromConfig() {
		Set<String> mmMobs = this.plugin.getConfig().getConfigurationSection("RaidMobs").getKeys(false); // only gets top keys
		Iterator<String> it = mmMobs.iterator();
		//converts set to arraylist
		while(it.hasNext()){
			mMMobNames.add(it.next());
	    }
		//gets chance and priority data for each mob name
		for (int k = 0; k < mMMobNames.size(); k++) {
			double chance = this.plugin.getConfig().getConfigurationSection("RaidMobs").getDouble(mMMobNames.get(k) + ".chance"); 
			int priority = this.plugin.getConfig().getConfigurationSection("RaidMobs").getInt(mMMobNames.get(k) + ".priority"); 
			chances.add(chance);
			priorities.add(priority);
		}
	}
	
	private static void spawnMobs(Random rand, List<Location>regionPlayerLocations, int scoreCounter, Score totalScore, MobManager mm, List<String> mMMobNames, List<Double> chances, List<Integer> priorities, int maxMobsPerPlayer) {
		for (int j = 0; j < playersInRegion.size(); j++) {
			int randomPlayerIdx = rand.nextInt(playersInRegion.size());
			World w = playersInRegion.get(j).getWorld();
			int x = regionPlayerLocations.get(randomPlayerIdx).getBlockX() + rand.nextInt(50) - 25;
			int y = regionPlayerLocations.get(randomPlayerIdx).getBlockY();
			int z = regionPlayerLocations.get(randomPlayerIdx).getBlockZ() + rand.nextInt(50) - 25;
	        String mythicMobName = "Marauder_Footman";
	        int spawnRate = rand.nextInt(2);
	        int numPlayersInRegion = playersInRegion.size();
	        int mobsAlive = mobsSpawned - scoreCounter - otherDeaths;
	        
	        if (mobsAlive >= numPlayersInRegion*maxMobsPerPlayer) {
	        	maxMobsReached = true; 	
	        } else {
	            maxMobsReached = false;
	        }
	        if (spawnRate == 1 && !maxMobsReached) {
	        	List<Integer> hitIdxs = new ArrayList<>();
	        	for (int k = 0; k < mMMobNames.size(); k++) {
	        		int randomNum = rand.nextInt(1000) + 1; // generates number between 1 and 1000
	        		if (randomNum <= chances.get(k)*1000) { // test for hit
	        			hitIdxs.add(k); //add hit index
	        		}
	        	}
	        	int maxPriority = 0;
	        	int maxPriorityIdx = 0;
	        	for (int n = 0; n < hitIdxs.size(); n++) {
	        		if (priorities.get(hitIdxs.get(n)) > maxPriority) { // does not account for same priority
	        			maxPriority = priorities.get(hitIdxs.get(n));
	        			maxPriorityIdx = hitIdxs.get(n);
	        		}
	        	}
	        	
	        	mythicMobName = mMMobNames.get(maxPriorityIdx); // set to first idx if nothing else hits
	        	
	        	Location zombieSpawnLocation = new Location(w, x, y, z);
	        	
				ActiveMob mob = mm.spawnMob(mythicMobName, zombieSpawnLocation);
				AbstractEntity entityOfMob = mob.getEntity();
				MmEntityList.add(entityOfMob);
				mobsSpawned++;
	        }
	        totalKills = scoreCounter;
	        totalScore.setScore(totalKills);
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender; // Convert sender into player
		World w = p.getWorld(); // Get world
		int maxMobsPerPlayer = this.plugin.getConfig().getInt("MaxMobsPerPlayer");
		
		//Check arguments
		//--------------------------------------------------------------------------------------------------------------
		if (args.length != 2) {
			p.sendMessage("Invalid arguments. Useage: /raid [region] [tier]");
			return false;
		}
		
		if (region != null) {
			p.sendMessage("There is already a raid in progress in region " + region.getId());
			p.sendMessage("To cancel this raid type /RaidsPerRegion cancel");
			return false;
		}
		
		com.sk89q.worldedit.world.World bukkitWorld = BukkitAdapter.adapt(w);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer(); // Put regions on
		RegionManager regions = container.get(bukkitWorld);
		region = regions.getRegion(args[0]);
		
		Map<String, ProtectedRegion> regionMap = regions.getRegions();
		if (!regionMap.containsKey(args[0])) {
			p.sendMessage("Invalid region. Useage: /raid [region] [tier]");
			return false;
		}
		
		if (args[1].contentEquals("1") || args[1].contentEquals("2") || args[1].contentEquals("3")) {
			int tier = Integer.parseInt(args[1]);
		} else {
			p.sendMessage("Invalid tier. Useage: /raid [region] [tier]");
			return false;
		}
		
		//-----------------------------------------------------------------------------------------------------------------------
		
		// variables to be reset for each raid
		timeReached = false;
		totalKills = 0;
		countdown = 120;
		mobsSpawned = 0;
		maxMobsReached = false;
		playersInRegion = new ArrayList<>();
		MmEntityList = new ArrayList<>();
		mMMobNames = new ArrayList<>();
		raidKills = new HashMap<String, Integer>();
		Main.cancelledRaid = false;
		runOnce = false;
		priorities = new ArrayList<>();
		chances = new ArrayList<>();
		mMMobNames = new ArrayList<>();
		otherDeaths = 0;
		//----------------------------------------------
		
		MobManager mm = MythicMobs.inst().getMobManager();
		region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.ALLOW);
		
		Scoreboard board = p.getScoreboard();
		Objective objective = board.registerNewObjective("raidKills", "dummy", "Raid Kills");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		Set<String> namesInScoreboard = new HashSet<String>();
		Score goalKills = objective.getScore((ChatColor.GOLD + "Goal:")); //Get a fake offline player
		Score totalScore = objective.getScore((ChatColor.RED + "Total Kills:")); //Get a fake offline player
		goalKills.setScore(goal);
		
		getMobsFromConfig();
		checkPlayersInRegion(region, board, objective, mm, totalScore, mMMobNames, chances, priorities, maxMobsPerPlayer);
		
		int[] id = {0};
		id[0] = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				
				if (!playersInRegion.isEmpty() && !runOnce) {
					runOnce = true;
					for (int n = 0; n < playersInRegion.size(); n++) { // Broadcasts title to every player in the raiding region
						send(playersInRegion.get(n), (ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tier " + args[1] + " Raid Underway"), (ChatColor.GOLD + "Prepare to fight!"), 10, 60, 10);
					}
				}
				
				
				// check for canceled raid
				if (Main.cancelledRaid) {
					for (int n = 0; n < playersInRegion.size(); n++) { // Broadcasts title to every player in the raiding region
						send(playersInRegion.get(n), (ChatColor.DARK_AQUA + " " + ChatColor.BOLD + "Raid Cancelled"), (ChatColor.GOLD + "Cancelled by an Administrator"), 10, 60, 10);
					}
					objective.unregister();
					timeReached = true;
					Bukkit.getServer().getScheduler().cancelTask(id[0]);
					region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
					region = null;
					for (int i = 0; i < MmEntityList.size(); i++) {
						if (MmEntityList.get(i).isLiving()) {
							MmEntityList.get(i).remove();
						}
					}
					return;
				} 
				//check for win
				if (totalKills >= goal) {
					for (int n = 0; n < playersInRegion.size(); n++) { // Broadcasts title to every player in the raiding region
						send(playersInRegion.get(n), (ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("RaidWinTitle"))), (ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("RaidWinSubtitle"))), 10, 30, 10);
					}
					objective.unregister();
					timeReached = true;
					Bukkit.getServer().getScheduler().cancelTask(id[0]);
					region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
					region = null;
					for (int i = 0; i < MmEntityList.size(); i++) {
						if (MmEntityList.get(i).isLiving()) {
							MmEntityList.get(i).damage(1000);
						}
					}
					return;
				}
				//check for loss
				if (countdown == 0) {
					timeReached = true;
					objective.unregister();
					region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
					if (totalKills < goal) {
						//raid lost
						for (int n = 0; n < playersInRegion.size(); n++) { // Broadcasts title to every player in the raiding region
							send(playersInRegion.get(n), (ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("RaidLoseTitle"))), (ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("RaidLoseSubtitle"))), 10, 30, 10);
						}
					}
					region = null;
					Bukkit.getServer().getScheduler().cancelTask(id[0]);
				}
				
				
				countdown--;
				Score timer = objective.getScore((ChatColor.GREEN + "Time:")); //Making a offline player called "Time:" with a green name and adding it to the scoreboard
				timer.setScore(countdown); //Making it so after "Time:" it displays the int countdown(So how long it has left in seconds.)
				
			}
		}, 0L, 20L); // repeats every second
		
		return false;
	}

}
