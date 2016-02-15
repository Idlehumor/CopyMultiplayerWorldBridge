package de.promolitor.copymultiplayerworldbridge;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.promolitor.copymultiplayerworld.CopyMultiplayerWorld;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public final class CopyMultiplayerWorldBridge extends JavaPlugin implements Listener{
	private static HashMap<String, ArrayList<int[]>> regionIds;
	private static HashMap<String, ArrayList<int[]>> chunkIds;
	
	@Override
	public void onEnable()
	{
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "cmw");
		this.regionIds = new HashMap<String, ArrayList<int[]>>();
		this.chunkIds = new HashMap<String, ArrayList<int[]>>();
		try {
			Class.forName("de.promolitor.copymultiplayerworld.CopyMultiplayerWorld");		
		} catch (ClassNotFoundException e) {
			this.getLogger().severe(
					"CopyMultiplayerWorld library not found! Be sure to put the latest version of CopyMultiplayerWorld on the mods folder!");
		}
	}
	
	@Override
	public boolean onCommand (CommandSender sender, Command command, String label, String[] args)
	{
		if ((sender instanceof Player) && command.getName().equals("cmw")) {
			Player player = (Player)sender;
			if (args.length == 4) {
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("radius")) {
					int currentChunkX = player.getLocation().getChunk().getX();
					int currentChunkZ = player.getLocation().getChunk().getZ();					
					ArrayList<int[]> localRIds = new ArrayList<int[]>();
					ArrayList<int[]> localCIds = new ArrayList<int[]>();
					
					int radius = 0;
					
					try {
						radius = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						player.sendMessage("The radius must be a number '/cmw dl radius <radius> <save-name>'");
						return true;
					}	

					int width = (radius*2)+1;
					for (int x = 0; x < width; x++) {
						for (int z = 0; z < width; z++) {
							int[] chunk = {currentChunkX - radius + x, currentChunkZ - radius + z};
							localCIds.add(chunk);
							int regionX = (currentChunkX - radius + x) >> 5;
							int regionZ = (currentChunkZ - radius + z) >> 5;
							int[] region = {regionX, regionZ};
							boolean isNotAddedYet = true;
							for (int[] rIds : localRIds) {
								if (rIds[0] == regionX && rIds[1] == regionZ) {
									isNotAddedYet = false;
								}
							}
							if (isNotAddedYet) {
								localRIds.add(region);
							}
						}
					}
					
					this.regionIds.put(player.getDisplayName(), localRIds);
					this.chunkIds.put(player.getDisplayName(), localCIds);
					
					if (checkChunksMessage(args[3], player)) {
						return true;
					}
				}
			} else if (args.length == 3) {
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("added")) {
					if (!checkChunksMessage(args[2], player)) {
						sender.sendMessage("You must first add chunks with '/cmw add'");
					}	
					return true;					
				}			
			} else if (args.length == 1) {
				if (args[0].toLowerCase().equals("add")) {
					String playerName = player.getDisplayName();
					int currentChunkX = player.getLocation().getChunk().getX();
					int currentChunkZ = player.getLocation().getChunk().getZ();
					
					ArrayList<int[]> localRIds = new ArrayList<int[]>();
					ArrayList<int[]> localCIds = new ArrayList<int[]>();						
					if (chunkIds.containsKey(playerName)) {
						localRIds = regionIds.get(playerName);
						localCIds = chunkIds.get(playerName);
					}
					
					int[] chunk = {currentChunkX, currentChunkZ};
					localCIds.add(chunk);
					int regionX = (currentChunkX) >> 5;
					int regionZ = (currentChunkZ) >> 5;
					int[] region = {regionX, regionZ};
					boolean isNotAddedYet = true;
					for (int[] rIds : localRIds) {
						if (rIds[0] == regionX && rIds[1] == regionZ) {
							isNotAddedYet = false;
						}
					}
					if (isNotAddedYet) {
						localRIds.add(region);
					}

					regionIds.put(playerName, localRIds);
					chunkIds.put(playerName, localCIds);
					sender.sendMessage("Chunk Added.");
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean checkChunksMessage(String saveName, Player player) {		
		if (regionIds.containsKey(player.getDisplayName()) && chunkIds.containsKey(player.getDisplayName())) {
			boolean isClaimed = true;
			for(int[] cIds : chunkIds.get(player.getDisplayName())) {			
				Claim claim = GriefPrevention.instance.dataStore.getClaimAt(new Location(player.getWorld(), cIds[0]*16, 0, cIds[1]*16), true, null);
				if (claim == null || (claim != null &&
					!claim.getOwnerName().equals(player.getDisplayName()) && 
					!claim.getOwnerName().equals(player.getUniqueId().toString()) && 
					!claim.getOwnerName().equals(player.getDisplayName().toLowerCase()))) {
					isClaimed = false;
				}
			}
			if (isClaimed) {				
				CopyMultiplayerWorld.instance.sendIds(player.getDisplayName(), saveName, regionIds.get(player.getDisplayName()), chunkIds.get(player.getDisplayName()));
			} else {
				player.sendMessage("You may only download chunks that you have claimed. Make sure you are standing in your claimed area.");
			}				
			regionIds.remove(player.getDisplayName());
			chunkIds.remove(player.getDisplayName());
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		regionIds.remove(event.getPlayer().getDisplayName());
		chunkIds.remove(event.getPlayer().getDisplayName());		
	}
	
}
