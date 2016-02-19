package de.promolitor.copymultiplayerworldbridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.lang.Math;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.promolitor.copymultiplayerworld.CopyMultiplayerWorld;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public final class CopyMultiplayerWorldBridge extends JavaPlugin implements Listener{
	private static HashMap<UUID, LinkedHashSet<int[]>> chunkIds;
	private static Claim gpClaim;
	private static FileConfiguration config;
	private static int chunkMargin = 0;
	private static int debugOutput = 0;
	
	@Override
	public void onEnable()
	{
		// Saves a new default config only if it does not exist
		this.saveDefaultConfig();
		// Loads the config
		config = this.getConfig();
		chunkMargin = config.getInt("chunk_margin");
		debugOutput = config.getInt("debug_output");
		chunkIds = new HashMap<UUID, LinkedHashSet<int[]>>();
		try {
			Class.forName("de.promolitor.copymultiplayerworld.CopyMultiplayerWorld");
		} catch (ClassNotFoundException e) {
			this.getLogger().severe(
					"CopyMultiplayerWorld library not found! Be sure to put the latest version of "
					+ "CopyMultiplayerWorld on the mods folder!");
			this.getPluginLoader().disablePlugin(this);
		}
	}
	
	@Override
	public boolean onCommand (CommandSender sender, Command command, String label, String[] args)
	{
		if ((sender instanceof Player) && command.getName().equals("cmw")) {
			Player player = (Player)sender;
			UUID pUUID = player.getUniqueId();
			if (args.length == 4) { 
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("radius")) {
					int currentChunkX = player.getLocation().getChunk().getX();
					int currentChunkZ = player.getLocation().getChunk().getZ();					
					LinkedHashSet<int[]> localRIds = new LinkedHashSet<int[]>();
					LinkedHashSet<int[]> localCIds = new LinkedHashSet<int[]>();
					
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
							localRIds.add(region);
						}
					}
					
					this.chunkIds.put(pUUID, localCIds);

					return checkChunksMessage(args[2], player);
				} 
			} else if (args.length == 3) {
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("claim")) {
					Claim claim = GriefPrevention.instance.dataStore.getClaimAt(((Player) sender).getLocation(), false, null);

					if (claim == null)
					{
						sender.sendMessage("You must be standing in a claim to use this command.");
						return true;
					}
					
					gpClaim = claim;

					return checkChunksMessage(args[2], player);
				} else if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("added")) {
					if (chunkIds.containsKey(pUUID)) {
						checkChunksMessage(args[2], player);
						return true;
					} else {
						sender.sendMessage("You must first add chunks with '/cmw add'");
					}	
					return true;					
				}		
			} else if (args.length == 6) {
                if(args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("area")) {				
					LinkedHashSet<int[]> localRIds = new LinkedHashSet<int[]>();
					LinkedHashSet<int[]> localCIds = new LinkedHashSet<int[]>();
                    int minCornerX = 0;
                    int minCornerZ = 0;
                    int maxCornerX = 0;
                    int maxCornerZ = 0;
                    try {
                    minCornerX = Integer.parseInt(args[3]);
                    minCornerZ = Integer.parseInt(args[4]);
                    maxCornerX = Integer.parseInt(args[5]);
                    maxCornerZ = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("The area must be a number '/cmw dl area <xcoordleastcorner> <zcoordleastcorner> <xcoordmaxcorner> <zcoordmaxcorner>'");
                        return true;
                    }   
                    if( minCornerX < 0) {
                        if (maxCornerX < 0) {
                        int centerX = Math.ceil(minCornerX/16) + Math.ceil(maxCornerX/16);
                        } else {
                          int centerX = Math.ceil(minCornerX/16) + Math.floor(maxCornerX/16);  
                        }
                    } else { 
                      if (maxCornerX < 0) {
                        int centerX = Math.floor(minCornerX/16) + Math.ceil(maxCornerX/16);
                        } else {
                          int centerX = Math.floor(minCornerX/16) + Math.floor(maxCornerX/16);  
                        }  
                    }
                        if( minCornerZ < 0) {
                            if (maxCornerZ < 0) {
                        int centerZ = Math.ceil(minCornerZ/16) + Math.ceil(maxCornerZ/16);
                        } else {
                          int centerZ = Math.ceil(minCornerZ/16) + Math.floor(maxCornerZ/16);  
                        }
                    } else { 
                      if (maxCornerZ < 0) {
                        int centerZ = Math.floor(minCornerZ/16) + Math.ceil(maxCornerZ/16);
                        } else {
                          int centerZ = Math.floor(minCornerZ/16) + Math.floor(maxCornerZ/16);  
                        }  
                    }
                    for (int x = 0; x < (Math.floor(Math.abs(minCornerX)/16) + Math.floor(Math.abs(maxCornerX)/16)); x++) {
                        for (int z = 0; z <(Math.abs(minCornerZ) + Math.abs(maxCornerZ)); z++) {
                            int[] chunk = {centerX - (Math.floor(Math.abs(minCornerX/16)) + Math.floor(Math.abs(maxCornerX)/16)) + x, centerZ - (Math.floor(Math.abs(minCornerZ)/16) + Math.floor(Math.abs(maxCornerZ)/16)) + z};
                            localCIds.add(chunk);
                            int regionX = (centerX - (Math.floor(Math.abs(minCornerX/16)) + Math.floor(Math.abs(maxCornerX)/16)) + x) >> 5;
							int regionZ = (centerZ - (Math.floor(Math.abs(minCornerX/16)) + Math.floor(Math.abs(maxCornerZ)/16)) + z) >> 5;
							int[] region = {regionX, regionZ};
							localRIds.add(region);
                        }
                    }
                    this.chunkIds.put(pUUID, localCIds);
                    return checkChunksMessage(args[2], player);
                }
            } else if (args.length == 1) {
				if (args[0].toLowerCase().equals("add")) {
					int currentChunkX = player.getLocation().getChunk().getX();
					int currentChunkZ = player.getLocation().getChunk().getZ();
					sendDebugOutput("Player standing in chunk "+"["+currentChunkX+","+currentChunkZ+"]");
					LinkedHashSet<int[]> localRIds = new LinkedHashSet<int[]>();
					LinkedHashSet<int[]> localCIds = new LinkedHashSet<int[]>();						
					if (chunkIds.containsKey(pUUID)) {
						localCIds = chunkIds.get(pUUID);
					}
					
					int[] chunk = {currentChunkX, currentChunkZ};
					localCIds.add(chunk);
					int regionX = (currentChunkX) >> 5;
					int regionZ = (currentChunkZ) >> 5;
					int[] region = {regionX, regionZ};
					localRIds.add(region);

					chunkIds.put(pUUID, localCIds);
					sender.sendMessage("Chunk Added.");
					return true;
				}
			}
		}
		return false;
	}
	
	private void sendDebugOutput(String message) {
		if (debugOutput == 1) { this.getLogger().info(message); }
	}
	
	private boolean checkChunksMessage(String saveName, Player player) {
		player.sendMessage("Checking for claims...");
		UUID pUUID = player.getUniqueId();
		if (chunkIds.containsKey(pUUID) || gpClaim != null) {
			boolean isClaimed = true;
			sendDebugOutput("CHUNK ARRAY SIZE:"+chunkIds.size());			
			sendDebugOutput("IS GP INSTANCE NULL?:"+(GriefPrevention.instance == null));
			sendDebugOutput("IS GP DATASTORE NULL?:"+(GriefPrevention.instance.dataStore == null));
			sendDebugOutput("PLAYER NAME:"+player.getName()+":UUID="+pUUID.toString());
			LinkedHashSet<int[]> claimChunks = chunkIds.get(pUUID);
			boolean bypassCheck = false;
			if (gpClaim != null && (gpClaim.ownerID.equals(pUUID) ||
					gpClaim.getOwnerName().equalsIgnoreCase(player.getName()) ||
					gpClaim.getOwnerName().equalsIgnoreCase(pUUID.toString()))) 
			{
				bypassCheck = true;	
				claimChunks = new LinkedHashSet<int[]>();
				ArrayList<Chunk> gpChunks = gpClaim.getChunks();
				for (Chunk chunk : gpChunks) {
					claimChunks.add(new int[]{chunk.getX(), chunk.getZ()});
				}
				claimChunks.addAll(this.getMarginIds(gpChunks));
				gpClaim = null;
			} else {
				if (gpClaim == null)
				{
		            for (int[] cIds : claimChunks) {
		                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(new Location(player.getWorld(), cIds[0] * 16, 0, cIds[1] * 16), true, null);
		                // player name check is required if an admin changes the name of the claim owner to an offline player
	                    if (claim == null) {
	                    	sendDebugOutput("CLAIM INFO|isNull=true:ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
	                    } else {
	
	                    	sendDebugOutput("CLAIM INFO|isNull=false:OwnerName=" + claim.getOwnerName() + ":ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
	                        if (claim.allowAccess(player) == null && 
	                        		claim.allowContainers(player) == null &&
	                        		claim.allowBuild(player, Material.STONE) == null)
	                        {
	                        	sendDebugOutput("CLAIM INFO|Success:OwnerName=" + claim.getOwnerName() + ":ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
	                        	isClaimed = true;
	                        }
		                }
		            }
				} else {
					sendDebugOutput("CLAIM INFO|isNull=false:OwnerName=" + gpClaim.getOwnerName());
                    if (gpClaim.allowAccess(player) == null && 
                    		gpClaim.allowContainers(player) == null &&
                    		gpClaim.allowBuild(player, Material.STONE) == null)
                    {
        				claimChunks = new LinkedHashSet<int[]>();
        				ArrayList<Chunk> gpChunks = gpClaim.getChunks();
        				for (Chunk chunk : gpChunks) {
        					claimChunks.add(new int[]{chunk.getX(), chunk.getZ()});
        				}
        				claimChunks.addAll(this.getMarginIds(gpChunks));
        				
        				sendDebugOutput("CLAIM INFO|Success:OwnerName=" + gpClaim.getOwnerName());
                    	isClaimed = true;
                    }
    				gpClaim = null;
				}
			}
			if (isClaimed || bypassCheck) {
				player.sendMessage("Claims Checked");
				player.sendMessage("Starting Download...");
				CopyMultiplayerWorld.instance.sendCoords(pUUID.toString(), saveName, claimChunks);
			} else {
				player.sendMessage("You may only download chunks that you have claimed or have build permissions in. Make sure you are standing in your claimed area.");
			}				
			chunkIds.remove(pUUID);
			return true;
		}
		return false;
	}
	
	private LinkedHashSet<int[]> getMarginIds(ArrayList<Chunk> chunks) {
		LinkedHashSet<int[]> marginIds = new LinkedHashSet<int[]>();
		
		if (chunkMargin == 0)
		{
			return marginIds;
		}
				
		int chunkCornerMinX = 0;
		int chunkCornerMinZ = 0;
		int chunkCornerMaxX = 0;
		int chunkCornerMaxZ = 0;
		
		
		for (Chunk chunk : chunks)
		{
			if (chunk.getX() < chunkCornerMinX) {
				chunkCornerMinX = chunk.getX();
			} else if (chunk.getX() > chunkCornerMaxX) {
				chunkCornerMaxX = chunk.getX();
			}
			if (chunk.getZ() < chunkCornerMaxX) {
				chunkCornerMaxX = chunk.getZ();				
			} else if (chunk.getZ() > chunkCornerMaxZ) {
				chunkCornerMaxZ = chunk.getZ();
			}
		}
		
		chunkCornerMinX -= chunkMargin;
		chunkCornerMinZ -= chunkMargin;
		chunkCornerMaxX += chunkMargin;
		chunkCornerMaxZ += chunkMargin;
		
		// Gets the min and max margin chunks for X
		for (int x = 0; x < chunkCornerMaxX-chunkCornerMinX; x++) {
			for (int z = 0; z < chunkMargin; z++) {
				marginIds.add(new int[]{chunkCornerMinX+x, chunkCornerMinZ+z});
				marginIds.add(new int[]{chunkCornerMinX+x, chunkCornerMaxZ-z});
			}
		}		
		// Gets the min and max margin chunks for Z
		for (int x = 0; x < chunkMargin; x++) {
			for (int z = chunkMargin; z < chunkCornerMaxZ-chunkCornerMinZ-chunkMargin; z++) {
				marginIds.add(new int[]{chunkCornerMinX+x, chunkCornerMinZ+z});
				marginIds.add(new int[]{chunkCornerMaxX-x, chunkCornerMaxZ+z});
			}
		}
		
		return marginIds;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		chunkIds.remove(event.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		chunkIds.remove(event.getPlayer().getUniqueId());	
	}
	
}
