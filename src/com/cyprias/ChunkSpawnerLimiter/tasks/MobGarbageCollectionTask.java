package com.cyprias.ChunkSpawnerLimiter.tasks;

import com.cyprias.ChunkSpawnerLimiter.ChatUtils;
import com.cyprias.ChunkSpawnerLimiter.Config;
import com.cyprias.ChunkSpawnerLimiter.Logger;
import com.cyprias.ChunkSpawnerLimiter.Plugin;
import com.cyprias.ChunkSpawnerLimiter.compare.MobGroupCompare;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MobGarbageCollectionTask implements Runnable {

    private Plugin plugin;

    public MobGarbageCollectionTask(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (final Chunk c : world.getLoadedChunks()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Stop processing quickly if this world is excluded from limits.
                        if (Config.getStringList("excluded-worlds").contains(c.getWorld().getName())) {
                            return;
                        }
                        Entity[] ents = c.getEntities();

                        HashMap<String, ArrayList<Entity>> types = new HashMap<String, ArrayList<Entity>>();
                        for (int i = ents.length - 1; i >= 0; i--) {
// ents[i].getType();
                            EntityType t = ents[i].getType();
                            String eType = t.toString();
                            String eGroup = MobGroupCompare.getMobGroup(ents[i]);
                            if (Config.contains("entities." + eType)) {
                                if (!types.containsKey(eType))
                                    types.put(eType, new ArrayList<Entity>());
                                types.get(eType).add(ents[i]);
                            }
                            if (Config.contains("entities." + eGroup)) {
                                if (!types.containsKey(eGroup))
                                    types.put(eGroup, new ArrayList<Entity>());
                                types.get(eGroup).add(ents[i]);
                            }
                        }
                        for (final Map.Entry<String, ArrayList<Entity>> entry : types.entrySet()) {
                            String eType = entry.getKey();
                            int limit = Config.getInt("entities." + eType);
// Logger.debug(c.getX() + " " + c.getZ() + ": " + eType + " = " +
// entry.getValue().size());
                            if (entry.getValue().size() > limit) {
                                Logger.debug("Removing " + (entry.getValue().size() - limit) + " " + eType + " @ " + c.getX() + " " + c.getZ());
                                if (Config.getBoolean("properties.notify-players")) {
                                    for (int i = ents.length - 1; i >= 0; i--) {
                                        if (ents[i] instanceof Player) {
                                            Player p = (Player) ents[i];
                                            ChatUtils.send(p, Config.getString("messages.removedEntites", entry.getValue().size() - limit, eType));
                                        }
                                    }
                                }
                                for (int i = entry.getValue().size() - 1; i >= limit; i--) {
                                    final int ii = i;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            entry.getValue().get(ii).remove();
                                        }
                                    }.runTask(plugin);
                                }
                            }
                        }
                    }
                }.runTaskAsynchronously(plugin);
            }
        }
    }
}
