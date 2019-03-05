package co.kolya.deathswap;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class GameManager {
	private final ArrayList<Game> games;
	public DeathSwap plugin;
	public FileConfiguration config;
	
	public GameManager(DeathSwap plugin) {
		this.games = new ArrayList<Game>();
		this.plugin = plugin;
		config = plugin.getConfig();
	}
	
	public void create(Player player) {
		for (Game game : this.games) {
			if (game.hasPlayer(player)) {
				player.sendMessage("Looks like you're already in a game! The current game has to end before you can start a new one.");
				return;
			}
			if (game.owner.equals(player)) {
				// Player has already started a game, make them use the end command
				player.sendMessage(
						String.format(
								"You already started a game! (Game ID: %s). Use \"/deathswap end\" to end it.",
								"" + ChatColor.YELLOW + ChatColor.BOLD + game.id
								)
						);
				return;
			}
		}
		
		// We can create a game
		Game newGame = new Game(player, this);
		this.games.add(newGame);
	}
	
	public void start(Player player) {
		for (Game game : this.games) {
			if (game.owner.equals(player)) {
				game.start();
				return;
			}
		}
		
		player.sendMessage("You haven't started a game of Death Swap. Start one with " + ChatColor.YELLOW + ChatColor.BOLD + "/deathswap create");
	}
	
	public void join(Player player, String gameId) {
		Game gameToJoin = null;
		
		for (Game game : this.games) {
			if (game.hasPlayer(player)) {
				player.sendMessage("Looks like you're already in a game! The current game has to end before you can start a new one.");
				return;
			}
			if (game.id.equalsIgnoreCase(gameId)) {
				gameToJoin = game;
			}
		}
		
		if (gameToJoin != null) {
			gameToJoin.join(player);
			return;
		}
		
		player.sendMessage("I can't seem to find the DeathSwap game " + ChatColor.YELLOW + gameId);
	}
	
	public void addPlayer(Player owner, String playerName) {
		Game ownersGame = null;
		for (Game game : this.games) {
			if (game.owner.equals(owner)) {
				ownersGame = game;
				break;
			}
 		}
		
		if (ownersGame == null) {
			owner.sendMessage("You must create a game before you can invite players.");
		}
		
		World world = owner.getWorld();
		List<Player> players = world.getPlayers();
		Player player = null;
		
		for (Player p : players) {
			if (p.getDisplayName().equalsIgnoreCase(playerName)) {
				player = p;
				break;
			}
		}
		
		if (player == null) {
			owner.sendMessage("Can't find that player.");
			return;
		}
		
		ownersGame.addPlayer(player);
	}
	
	public void end(Player player) {
		for (Game game : this.games) {
			if (game.owner.equals(player)) {
				game.end();
				games.remove(game);
				return;
			}
		}
		
		player.sendMessage("You aren't currently in a game of Death Swap. Start one with " + ChatColor.YELLOW + ChatColor.BOLD + "/deathswap start");
	}
	
	public void end(Game game) {
		games.remove(game);
	}
	
	public void handleDeath(Player player) {
		for (Game game : this.games ) {
			if (game.hasPlayer(player)) {
				game.markPlayerAsDead(player);
				return;
			}
		}
	}
	
	public void handleQuit(Player player) {
		for (Game game : this.games ) {
			if (game.owner.equals(player)) {
				game.end();
				return;
			}
			
			if (game.hasPlayer(player)) {
				game.markPlayerAsDead(player);
				return;
			}
		} 
	}
}
