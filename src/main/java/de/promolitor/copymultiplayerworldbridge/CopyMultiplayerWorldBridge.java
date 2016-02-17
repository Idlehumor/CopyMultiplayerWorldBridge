package de.promolitor.copymultiplayerworldbridge;

import de.promolitor.copymultiplayerworld.CopyMultiplayerWorld;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class CopyMultiplayerWorldBridge extends JavaPlugin implements Listener{
	private static HashMap<UUID, ArrayList<int[]>> regionIds;
	private static HashMap<UUID, ArrayList<int[]>> chunkIds;
	
	@Override
	public void onEnable()
	{
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "cmw");
		// todo you might want to use a Set instead of list, so there is no need to check for duplicates (to be specific LinkedHashSet for iteration speed)
		regionIds = new HashMap<UUID, ArrayList<int[]>>();
		chunkIds = new HashMap<UUID, ArrayList<int[]>>();
		try {
			Class.forName("de.promolitor.copymultiplayerworld.CopyMultiplayerWorld");
		} catch (ClassNotFoundException e) {
			this.getLogger().severe(
					"CopyMultiplayerWorld library not found! Be sure to put the latest version of CopyMultiplayerWorld on the mods folder!");
            //todo disable plugin
		}
	}
	
	@Override
	public boolean onCommand (CommandSender sender, Command command, String label, String[] args)
	{
		if ((sender instanceof Player) && command.getName().equals("cmw")) {
			Player player = (Player)sender;
			if (args.length == 3) {
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("claim")) {
					Claim claim = GriefPrevention.instance.dataStore.getClaimAt(((Player) sender).getLocation(), false, null);
                    //todo check if player has build permission on claim
					ArrayList<Chunk> chunks = claim.getChunks();

					Set<int[]> localRIds = new HashSet<>();
					Set<int[]> localCIds = new HashSet<>();

                    // todo configurable margin
					for(Chunk chunk : chunks) {
						localCIds.add(new int[]{chunk.getX(), chunk.getZ()});
						localRIds.add(new int[]{chunk.getX() >> 5, chunk.getZ() >> 5});
					}
					chunkIds.put(player.getUniqueId(), new ArrayList<>(localCIds));
					regionIds.put(player.getUniqueId(), new ArrayList<>(localRIds));

                    if (checkChunksMessage(args[2], player, false)) {
                        return true;
                    }
				}
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
					
					this.regionIds.put(player.getUniqueId(), localRIds);
					this.chunkIds.put(player.getUniqueId(), localCIds);
					
					if (checkChunksMessage(args[3], player, true)) {
						return true;
					}
				}
			} else if (args.length == 3) {
				if (args[0].toLowerCase().equals("dl") && args[1].toLowerCase().equals("added")) {
					if (!checkChunksMessage(args[2], player, true)) {
						sender.sendMessage("You must first add chunks with '/cmw add'");
					}	
					return true;					
				}			
			} else if (args.length == 1) {
				if (args[0].toLowerCase().equals("add")) {
					UUID pUUID = player.getUniqueId();
					int currentChunkX = player.getLocation().getChunk().getX();
					int currentChunkZ = player.getLocation().getChunk().getZ();
					// DEBUG
					this.getLogger().info("Player standing in chunk "+"["+currentChunkX+","+currentChunkZ+"]");
					ArrayList<int[]> localRIds = new ArrayList<int[]>();
					ArrayList<int[]> localCIds = new ArrayList<int[]>();						
					if (chunkIds.containsKey(pUUID)) {
						localRIds = regionIds.get(pUUID);
						localCIds = chunkIds.get(pUUID);
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

					regionIds.put(pUUID, localRIds);
					chunkIds.put(pUUID, localCIds);
					sender.sendMessage("Chunk Added.");
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean checkChunksMessage(String saveName, Player player, boolean checkClaim) {
		if (regionIds.containsKey(player.getUniqueId()) && chunkIds.containsKey(player.getUniqueId())) {
			boolean isClaimed = true;
			// DEBUG
			this.getLogger().info("CHUNK ARRAY SIZE:"+chunkIds.size());			
			this.getLogger().info("IS GP INSTANCE NULL?:"+(GriefPrevention.instance == null));
			this.getLogger().info("IS GP DATASTORE NULL?:"+(GriefPrevention.instance.dataStore == null));
			this.getLogger().info("PLAYER DISPLAY NAME"+player.getUniqueId()+":UUID="+player.getUniqueId().toString());
			if (checkClaim) {
                for (int[] cIds : chunkIds.get(player.getUniqueId())) {
                    Claim claim = GriefPrevention.instance.dataStore.getClaimAt(new Location(player.getWorld(), cIds[0] * 16, 0, cIds[1] * 16), true, null);
                    if (claim == null ||
                            !(claim.ownerID.equals(player.getUniqueId()) ||
                                    claim.getOwnerName().equalsIgnoreCase(player.getUniqueId().toString()))) {
                        isClaimed = false;
                        if (claim == null) {
                            // DEBUG
                            this.getLogger().info("CLAIM INFO|isNull=true:ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
                        } else {
                            // DEBUG
                            this.getLogger().info("CLAIM INFO|isNull=false:OwnerName=" + claim.getOwnerName() + ":ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
                        }
                    } else {
                        // DEBUG
                        this.getLogger().info("CLAIM INFO|Success:OwnerName=" + claim.getOwnerName() + ":ClaimAtLocationCheck=" + (cIds[0] * 16) + "," + (cIds[1] * 16));
                    }
                }
            }
			if (!checkClaim || isClaimed) {
				CopyMultiplayerWorld.instance.sendIds(player.getName(), saveName, regionIds.get(player.getUniqueId()), chunkIds.get(player.getUniqueId()));
			} else {
				player.sendMessage("You may only download chunks that you have claimed. Make sure you are standing in your claimed area.");
			}				
			regionIds.remove(player.getUniqueId());
			chunkIds.remove(player.getUniqueId());
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		regionIds.remove(event.getPlayer().getUniqueId());
		chunkIds.remove(event.getPlayer().getUniqueId());		
	}
	
}
