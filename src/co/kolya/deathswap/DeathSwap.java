package co.kolya.deathswap;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DeathSwap extends JavaPlugin implements Listener {
	private final GameManager gameManager;
	
	public DeathSwap() {
		this.gameManager = new GameManager(this);
	}
	
	@Override
	public void onEnable() {
		getLogger().info("DeathSwap loaded");
		Bukkit.getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
	}
	
	@Override
	public void onDisable() {
		getLogger().info("DeathSwap unloaded");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			String name = cmd.getName();
			if (name.equalsIgnoreCase("deathswap") || name.equalsIgnoreCase("ds")) {
				if (args.length == 0) {
					return false;
				}
				String command = args[0];
				Player player = (Player)sender;
				
				if (command.equalsIgnoreCase("create")) {
					this.gameManager.create(player);
				} else if (command.equalsIgnoreCase("start")) {
					this.gameManager.start(player);
				} else if (command.equalsIgnoreCase("join")) {
					if (args.length >= 2) {
						String gameId = args[1];
						this.gameManager.join(player, gameId);
					} else {
						sender.sendMessage("You must provide a game ID to join a game!");
					}
				} else if (command.equalsIgnoreCase("end")) {
					this.gameManager.end(player);
				} else if (command.equalsIgnoreCase("addPlayer")) {
					if (args.length >= 2) {
						String playerName = args[1];
						this.gameManager.addPlayer(player, playerName);
					} else {
						sender.sendMessage("You must provide a player name to add them to your game!");
					}
			    } else if(command.equalsIgnoreCase("help")) {
			    	sender.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE + "Death Swap Help");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "create" + ChatColor.RESET + " - Creates a new Death Swap game");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "start" + ChatColor.RESET + " - Start the current Death Swap game (Owner only)");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "end" + ChatColor.RESET + " - Forces your Death Swap game to end (Owner only)");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "join <game ID>" + ChatColor.RESET + " - Joins a Death Swap game, using the provided ID");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "addPlayer <username>" + ChatColor.RESET + " - Adds the given user to your game (Owner only)");
			    	sender.sendMessage("    " + ChatColor.YELLOW + "help" + ChatColor.RESET + " - Show this help message");
			    } else {
					return false;
				}
				return true;
			}
		} else {
			sender.sendMessage("DeathSwap games can only be managed by players.");
		}
		return false;
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		this.gameManager.handleDeath(player);
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		this.gameManager.handleQuit(player);
	}
}
