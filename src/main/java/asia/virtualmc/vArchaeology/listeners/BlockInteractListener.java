package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import io.lumine.mythic.bukkit.MythicBukkit;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockInteractListener implements Listener {

    private final Main plugin;
    private Location salvageStationLocation;

    public BlockInteractListener(Main plugin) {
        this.plugin = plugin;
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
            String locationString = String.format("%d, %d, %d", blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
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
            try {
                // Get the skill name from config
                String skillName = plugin.getConfig().getString("settings.mythicmobs.skill-name", "MythicMobSkillName");

                // Get the skill manager
                SkillExecutor skillManager = MythicBukkit.inst().getSkillManager();

                // Get and execute the skill
                Skill skill = skillManager.getSkill(skillName).orElse(null);

                if (skill != null) {
                    // Execute the skill at the player's location
                    skill.execute();
                    skill.execute(BukkitAdapter.adapt(player), BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), 1.0f);
                    player.sendMessage("You activated the salvage station!");
                } else {
                    player.sendMessage("This salvage station seems to be malfunctioning...");
                    plugin.getLogger().warning("Skill '" + skillName + "' not found in MythicMobs configuration!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing MythicMobs skill: " + e.getMessage());
                e.printStackTrace();
            }

            event.setCancelled(true);
        }
    }

    private String formatLocation(Location location) {
        return String.format("(%.1f, %.1f, %.1f)", location.getX(), location.getY(), location.getZ());
    }
}