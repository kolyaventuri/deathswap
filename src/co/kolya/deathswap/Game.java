package co.kolya.deathswap;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class Game {
	private final int idLength = 5;
	private int minSwapDuration;
	private int maxSwapDuration;
	private int swapRadius;
	
	private GameManager gameManager;
	private BukkitTask swapTimer = null;
	
	public Player owner;
	public String id;
	private Location startLocation;
	
	private ArrayList<Player> players;
	private ArrayList<Player> deadPlayers;
	
	public Game(Player owner, GameManager gameManager) {
		this.gameManager = gameManager;
		this.owner = owner;
		this.players = new ArrayList<Player>();
		this.deadPlayers = new ArrayList<Player>();
		this.id = IDGenerator.random(this.idLength);
		
		FileConfiguration config = gameManager.config;
		
		this.minSwapDuration = config.getInt("minSwapDuration");
		this.maxSwapDuration = config.getInt("maxSwapDuration");
		this.swapRadius = config.getInt("swapRadius");
		
		this.players.add(owner);
		
		owner.sendMessage("Starting up a new game of Death Swap!");
		owner.sendMessage("Game started! Your friends can join using " + ChatColor.YELLOW + ChatColor.BOLD + "/deathswap join " + this.id);
	}
	
	public void start() {
		if (this.players.size() == 1) {
			this.owner.sendMessage("You cannot start a Death Swap game with only 1 player.");
			return;
		} 
		
		this.startLocation = this.owner.getLocation();
		
		clearStats();
		blindEveryone();
		randomizeLocations();
		sendCountdown();
	}
	
	public void join(Player player) {
		this.players.add(player);
		broadcast(player.getDisplayName() + " has joined the game!");
	}
	
	public void end() {
		broadcast("Ending this game of Death Swap... Thanks for playing!");
		if (this.swapTimer != null) {
			this.swapTimer.cancel();
		}
		this.gameManager.end(this);
	}
	
	public boolean hasPlayer(Player player) {
		for (Player p : this.players ) {
			if (p.equals(player)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void markPlayerAsDead(Player player) {
		this.deadPlayers.add(player);
		player.teleport(this.startLocation);
		
		if (this.deadPlayers.size() >= this.players.size() - 1) {
			// Player has won
			Player winner = null;
			
			for (Player p : this.players) {
				if (!this.deadPlayers.contains(p)) {
					winner = p;
					p.teleport(this.startLocation);
				}
			}
			
			broadcastTitle("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + winner.getDisplayName() + ChatColor.RESET + " wins!", "", 10, 70, 20);
			end();
		}
	}
	
	private void clearStats() {
		for (Player player : this.players) {
			player.setHealth(20);
			
			Collection<PotionEffect> potions = player.getActivePotionEffects();
			for (PotionEffect potion : potions) {
				player.removePotionEffect(potion.getType());
			}
			
			player.getInventory().clear();
		}
	}
	
	private void blindEveryone() {
		for (Player player : this.players) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*6, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20*5, 10));
		}
	}
	
	private String buildSubtitle(int timeLeft) {
		return String.format("The game is starting in " + ChatColor.YELLOW + ChatColor.BOLD + "%d" + ChatColor.RESET + "...", timeLeft);
	}
	
	private void sendCountdown() {
		String title = ChatColor.GREEN + "Get Ready!";
		
		new BukkitRunnable() {
			int timeLeft = 5;
			
			@Override
			public void run() {
				if (timeLeft < 1) {
					broadcastTitle("" + ChatColor.GREEN + ChatColor.BOLD + "GO!", "", 0, 70, 20);
					this.cancel();
					startSwapTimer();
					return;
				}
				broadcastTitle(title, buildSubtitle(timeLeft), 0, 25, 0);
				
				timeLeft--;
			}
		}.runTaskTimer(this.gameManager.plugin, 0L, 20L);
	}
	
	private void randomizeLocations() {
		Location[] locations = getStartingPoints(this.players.size());
		
		for (int i = 0; i < locations.length; i++) {
			Player player = this.players.get(i);
			player.teleport(locations[i]);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 30, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 30, 1));
		}
	}
	
	private Location[] getStartingPoints(int playerCount) {
		Point[] points = new Point[playerCount];
		World world = this.owner.getWorld();
		
		Location origin = this.owner.getLocation();
		int x = origin.getBlockX();
		int y = origin.getBlockZ(); // Y is up technically, but y will be easier to visualize in code
		int radius = this.swapRadius;
		
		double slice = 2 * Math.PI / playerCount;
		
		for (int i = 0; i < playerCount; i++) {
			double angle = slice * i;
			
			int newX = x + (int)(radius * Math.cos(angle));
			int newY = y + (int)(radius * Math.sin(angle));
			
			this.owner.sendMessage(newX + ", " + newY);
			points[i] = new Point(newX, newY);
		}
		
		// Resolve to block locations
		Location[] locations = new Location[playerCount];
		for (int i = 0; i < points.length; i++) {
			Point point = points[i];
			locations[i] = new Location(world, point.getX(), point.getY(), 0);
			
			// Don't want to spawn inside a block so we get the highest and add 1
			locations[i] = world.getHighestBlockAt(locations[i]).getLocation();
		}
		
		return locations;
	}
	
	private void startSwapTimer() {
		swapTimer = new BukkitRunnable() {
			int timeLeft = getRandomTime();
			
			@Override
			public void run() {
				owner.sendMessage(timeLeft + " seconds left");
				if (timeLeft < 1) {
					performSwap();
					this.cancel();
					startSwapTimer();
					return;
				}
				
				timeLeft--;
			}
		}.runTaskTimer(this.gameManager.plugin, 0L, 20L);
	}
	
	private void performSwap() {
		ArrayList<Player> swappable = new ArrayList<Player>();
		
		for (Player player : this.players) {
			if (!this.deadPlayers.contains(player)) {
				player.sendTitle("" + ChatColor.YELLOW + ChatColor.BOLD + "Swapping!", "", 10, 40, 20);
				swappable.add(player);
			}
		}
		
		Player[] players = swappable.toArray(new Player[swappable.size()]);
		Location[] points = new Location[players.length];
		for (int i = 0; i < players.length; i++) {
			points[i] = players[i].getLocation();
		}
		
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			int index = i + 1;
			if (index == players.length) {
				index = 0;
			}
			
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 5, 1));
			player.teleport(points[index]);
		}
	}
	
	private int getRandomTime() {
		return ThreadLocalRandom.current().nextInt(minSwapDuration, maxSwapDuration + 1);
	}
	
	private void broadcast(String message) {
		for (Player player : this.players) {
			player.sendMessage(message);
		}
	}
	
	private void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		for(Player player : this.players) {
			player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
		}
	}
}