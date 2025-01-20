package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockInteractListener implements Listener {

    private final Main plugin;
    private Location salvageStationLocation;
    private final Map<UUID, Hologram> activeHolograms;
    private final Map<UUID, BukkitRunnable> activeCraftingTasks;

    public BlockInteractListener(Main plugin) {
        this.plugin = plugin;
        this.activeHolograms = new HashMap<>();
        this.activeCraftingTasks = new HashMap<>();
        loadSalvageStation();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadSalvageStation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("settings.interactableBlocks.salvage-station")) {
            String[] coords = config.getString("settings.interactableBlocks.salvage-station").split(",");
            try {
                double x = Double.parseDouble(coords[0].trim());
                double y = Double.parseDouble(coords[1].trim());
                double z = Double.parseDouble(coords[2].trim());
                this.salvageStationLocation = new Location(plugin.getServer().getWorlds().get(0), x, y, z);

                if (x == 0 && y == 0 && z == 0) {
                    plugin.getLogger().info("Salvage station coordinates are (0, 0, 0). Block will not be created.");
                    salvageStationLocation = null;
                } else {
                    ensureBlockExists();
                }

            } catch (NumberFormatException e) {
                plugin.getLogger().severe("Invalid salvage station coordinates in config.yml");
            }
        } else {
            plugin.getLogger().warning("No salvage station location found in config.yml.");
            salvageStationLocation = null;
        }
    }

    private void ensureBlockExists() {
        if (salvageStationLocation != null) {
            Block block = salvageStationLocation.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.STONE);
                plugin.getLogger().info("Created salvage station block at " + formatLocation(salvageStationLocation));
            }
        }
    }

    public void createSalvageStation(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null) {
            Location blockLocation = targetBlock.getLocation();
            FileConfiguration config = plugin.getConfig();
            String locationString = String.format("%d, %d, %d",
                    blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
            config.set("settings.interactableBlocks.salvage-station", locationString);
            plugin.saveConfig();

            plugin.getLogger().info("Saved new salvage station location: " + locationString);
            loadSalvageStation();
        } else {
            player.sendMessage("No block in view to set as a salvage station!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();

        if (salvageStationLocation != null && clickedBlock.getLocation().equals(salvageStationLocation)) {
            event.setCancelled(true);

            if (activeCraftingTasks.containsKey(player.getUniqueId())) {
                player.sendMessage("You are already processing something!");
                return;
            }

            startCrafting(player);
        }
    }

    private void startCrafting(Player player) {
        Location holoLocation = salvageStationLocation.clone().add(0.5, 1.5, 0.5);
        String holoName = "progress_hologram_" + player.getUniqueId();

        String[] progressChars = {
                "\uE0F2", // Stage 1
                "\uE0F3", // Stage 2
                "\uE0F4", // Stage 3
                "\uE0F5", // Stage 4
                "\uE0F6", // Stage 5
                "\uE0F7", // Stage 6
                "\uE0F8", // Stage 7
                "\uE0F9"  // Stage 8
        };
        ArrayList<String> lines = new ArrayList<>();
        lines.add(progressChars[0]); // Start with first character

        try {
            Hologram hologram = DHAPI.createHologram(holoName, holoLocation, lines);
            hologram.setDefaultVisibleState(false);
            hologram.setShowPlayer(player);

            activeHolograms.put(player.getUniqueId(), hologram);

            BukkitRunnable craftingTask = new BukkitRunnable() {
                private int secondsPassed = 0;

                @Override
                public void run() {
                    secondsPassed++;

                    if (secondsPassed <= 8) {
                        // Update using DHAPI
                        DHAPI.setHologramLine(hologram, 0, progressChars[secondsPassed - 1]);

                    } else if (secondsPassed == 9) {
                        // Clear and show diamond
                        DHAPI.removeHologramLine(hologram, 0);
                        DHAPI.addHologramLine(hologram, Material.DIAMOND);
                        //DHAPI.addHologramLine(hologram, new ItemStack(Material.DIAMOND));

                    } else {
                        // Cleanup and reward
                        DHAPI.removeHologram(holoName);
                        activeHolograms.remove(player.getUniqueId());

                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
                        player.sendMessage("You received a diamond!");

                        activeCraftingTasks.remove(player.getUniqueId());
                        this.cancel();
                    }
                }
            };

            activeCraftingTasks.put(player.getUniqueId(), craftingTask);
            craftingTask.runTaskTimer(plugin, 0L, 20L);

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            player.sendMessage("Error creating hologram. Please contact an administrator.");
        }
    }

    public void cleanup() {
        for (BukkitRunnable task : activeCraftingTasks.values()) {
            task.cancel();
        }
        activeCraftingTasks.clear();

        for (Hologram hologram : activeHolograms.values()) {
            DHAPI.removeHologram(hologram.getName());
        }
        activeHolograms.clear();
    }

    private String formatLocation(Location location) {
        return String.format("(%.1f, %.1f, %.1f)",
                location.getX(), location.getY(), location.getZ());
    }
}