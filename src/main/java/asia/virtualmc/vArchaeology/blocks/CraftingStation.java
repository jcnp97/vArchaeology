package asia.virtualmc.vArchaeology.blocks;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.CustomItems;
import asia.virtualmc.vArchaeology.items.CustomTools;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.*;

public class CraftingStation implements Listener {

    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final PlayerData playerData;
    private final Statistics statistics;
    private final CustomTools customTools;
    private final ConfigManager configManager;
    private final CustomItems customItems;
    private Location craftingStationLocation;
    private final Map<UUID, Hologram> activeHolograms;
    private final Map<UUID, BukkitRunnable> activeCraftingTasks;
    private final Map<UUID, Long> cooldowns;
    private final NamespacedKey BRONZE_MATTOCK;
    private final NamespacedKey TOOL_KEY;

    // Permanent hologram for the crafting station (title and lore)
    private Hologram permanentHologram;

    public CraftingStation(Main plugin,
                           EffectsUtil effectsUtil,
                           PlayerData playerData,
                           Statistics statistics,
                           CustomTools customTools,
                           ConfigManager configManager,
                           CustomItems customItems) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.playerData = playerData;
        this.statistics = statistics;
        this.customTools = customTools;
        this.configManager = configManager;
        this.customItems = customItems;
        this.activeHolograms = new HashMap<>();
        this.activeCraftingTasks = new HashMap<>();
        this.cooldowns = new HashMap<>();
        this.TOOL_KEY = new NamespacedKey(plugin, "varch_tool");
        this.BRONZE_MATTOCK = new NamespacedKey(plugin, "bronze_mattock");

        loadCraftingStation();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Register Bronze Mattock recipe
        ItemStack bronzeMattock = customTools.getArchToolCache(1);
        ShapedRecipe tier1Recipe = new ShapedRecipe(BRONZE_MATTOCK, bronzeMattock);
        tier1Recipe.shape("AAB", " C ", " C ");
        tier1Recipe.setIngredient('A', Material.IRON_INGOT);
        tier1Recipe.setIngredient('B', Material.COPPER_INGOT);
        tier1Recipe.setIngredient('C', Material.STICK);
        plugin.getServer().addRecipe(tier1Recipe);
    }

    private void loadCraftingStation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("settings.interactableBlocks.crafting-station")) {
            String[] coords = config.getString("settings.interactableBlocks.crafting-station").split(",");
            try {
                double x = Double.parseDouble(coords[0].trim());
                double y = Double.parseDouble(coords[1].trim());
                double z = Double.parseDouble(coords[2].trim());
                this.craftingStationLocation = new Location(plugin.getServer().getWorlds().get(0), x, y, z);

                if (x == 0 && y == 0 && z == 0) {
                    plugin.getLogger().severe("Crafting station coordinates are (0, 0, 0). Block will not be created.");
                    craftingStationLocation = null;
                } else {
                    ensureBlockExists();
                    createPermanentHologram();
                }

            } catch (NumberFormatException e) {
                plugin.getLogger().severe("Invalid crafting station coordinates in config.yml");
            }
        } else {
            plugin.getLogger().severe("No crafting station location found in config.yml.");
            craftingStationLocation = null;
        }
    }

    private void ensureBlockExists() {
        if (craftingStationLocation != null) {
            Block block = craftingStationLocation.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.STONE);
                plugin.getLogger().info("Created crafting station block at " + formatLocation(craftingStationLocation));
            }
        }
    }

    private void createPermanentHologram() {
        if (craftingStationLocation == null) return;

        if (permanentHologram != null) {
            DHAPI.removeHologram(permanentHologram.getName());
        }

        Location permanentHoloLocation = craftingStationLocation.clone().add(0.5, 1.5, 0.5);
        List<String> lines = Arrays.asList(
                "§6Crafting Station §f\uE073"
        );
        permanentHologram = DHAPI.createHologram("crafting_station", permanentHoloLocation, lines);
        permanentHologram.setDefaultVisibleState(true);
    }

    public void createCraftingStation(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null) {
            Location blockLocation = targetBlock.getLocation();
            FileConfiguration config = plugin.getConfig();
            String locationString = String.format("%d, %d, %d",
                    blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
            config.set("settings.interactableBlocks.crafting-station", locationString);
            plugin.saveConfig();

            plugin.getLogger().info("Saved new crafting station location: " + locationString);
            loadCraftingStation();
        } else {
            player.sendMessage("§cNo block in view to set as a crafting station!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (craftingStationLocation != null && clickedBlock.getLocation().equals(craftingStationLocation)) {
            event.setCancelled(true);

            long currentTime = System.currentTimeMillis();
            if (cooldowns.containsKey(uuid)) {
                long lastInteraction = cooldowns.get(uuid);
                if (currentTime - lastInteraction < 1000) {
                    return;
                }
            }
            cooldowns.put(uuid, currentTime);

            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (mainHandItem.getType() == Material.AIR) {
                return;
            }

            if (mainHandItem.getType() == Material.STICK) {
                openBronzeMattockRecipe(player);
                return;
            }

            ItemMeta meta = mainHandItem.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!mainHandItem.hasItemMeta() || !pdc.has(TOOL_KEY, PersistentDataType.INTEGER)) {
                player.sendMessage("§cYou can only use STICK or Archaeology tools here!");
                return;
            }

            if (activeCraftingTasks.containsKey(uuid)) {
                player.sendMessage("§cYou are currently engaged in a crafting process!");
                return;
            }

            int toolID = customTools.getToolId(mainHandItem);

            if (levelRequirementsCheck(uuid, toolID) && toolID < 10) {
                openCraftingStation(player, toolID);
            } else {
                player.sendMessage("§cYou don't have the required level to upgrade this mattock!");
            }
        }
    }

    private boolean levelRequirementsCheck(UUID uuid, int toolID) {
        int archLevel = playerData.getArchLevel(uuid);
        return switch (toolID) {
            case 1 -> archLevel >= 15;
            case 2 -> archLevel >= 30;
            case 3 -> archLevel >= 40;
            case 4 -> archLevel >= 50;
            case 5 -> archLevel >= 60;
            case 6 -> archLevel >= 70;
            case 7 -> archLevel >= 80;
            case 8 -> archLevel >= 90;
            case 9 -> archLevel >= 99;
            default -> false;
        };
    }

    /**
     * Starts the crafting process for a player.
     * Removes the permanent hologram (if it exists) so that a progress hologram can be shown.
     * Once the process is complete (and if no other crafting tasks are running), the permanent hologram is recreated.
     */
    private void startCrafting(Player player, int toolID) {
        // Remove the permanent hologram temporarily (if it exists).
        if (permanentHologram != null) {
            permanentHologram.setHidePlayer(player);
        }

        Location holoLocation = craftingStationLocation.clone().add(0.5, 1.5, 0.5);
        UUID uuid = player.getUniqueId();
        String holoName = "progress_hologram_" + uuid;

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
        lines.add(progressChars[0]);

        try {
            Hologram hologram = DHAPI.createHologram(holoName, holoLocation, lines);
            hologram.setDefaultVisibleState(false);
            hologram.setShowPlayer(player);

            activeHolograms.put(uuid, hologram);
            effectsUtil.playSoundUUID(uuid, "minecraft:cozyvanilla.restoration_sounds", Sound.Source.PLAYER, 1.0f, 1.0f);

            BukkitRunnable craftingTask = new BukkitRunnable() {
                private int secondsPassed = 0;

                @Override
                public void run() {
                    secondsPassed++;

                    if (secondsPassed <= 8) {
                        DHAPI.setHologramLine(hologram, 0, progressChars[secondsPassed - 1]);
                    } else if (secondsPassed == 9) {
                        DHAPI.removeHologramLine(hologram, 0);
                        String hologramItem = "#ICON: " + customTools.getMaterialName(toolID + 1) + " {CustomModelData:" + customTools.getToolModelData(toolID + 1) + "}";
                        DHAPI.addHologramLine(hologram, hologramItem);
                    } else {
                        DHAPI.removeHologram(holoName);
                        activeHolograms.remove(uuid);

                        effectsUtil.spawnFireworks(uuid, 10, 3);
                        effectsUtil.sendTitleMessage(uuid, "<#7CFEA7>You have upgraded your", EffectsUtil.convertLegacy(customTools.getDisplayName(toolID)) +
                                " <#7CFEA7>→ " + EffectsUtil.convertLegacy(customTools.getDisplayName(toolID + 1)));
                        customTools.giveArchTool(uuid, toolID + 1);

                        activeCraftingTasks.remove(uuid);
                        permanentHologram.removeHidePlayer(player);
                        this.cancel();
                    }
                }
            };

            activeCraftingTasks.put(uuid, craftingTask);
            craftingTask.runTaskTimer(plugin, 0L, 20L);

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            player.sendMessage("Error creating hologram. Please contact an administrator.");
        }
    }

    public void cleanupAllCooldowns() {
        for (BukkitRunnable task : activeCraftingTasks.values()) {
            task.cancel();
        }
        activeCraftingTasks.clear();

        for (Hologram hologram : activeHolograms.values()) {
            DHAPI.removeHologram(hologram.getName());
        }
        activeHolograms.clear();
        cooldowns.clear();
    }

    private String formatLocation(Location location) {
        return String.format("(%.1f, %.1f, %.1f)",
                location.getX(), location.getY(), location.getZ());
    }

    // GUI Methods

    public void openBronzeMattockRecipe(Player player) {
        ChestGui gui = new ChestGui(6, configManager.craftingStationTitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 6);

        // IRON_INGOT
        for (int x = 1; x <= 2; x++) {
            ItemStack ironIngot = new ItemStack(Material.IRON_INGOT);
            GuiItem iron = new GuiItem(ironIngot);
            staticPane.addItem(iron, x, 1);
        }

        // COPPER_INGOT
        ItemStack copperIngot = new ItemStack(Material.COPPER_INGOT);
        GuiItem copper = new GuiItem(copperIngot);
        staticPane.addItem(copper, 3, 1);

        // STICK
        for (int x = 2; x <= 3; x++) {
            ItemStack stick = new ItemStack(Material.STICK);
            GuiItem stickGUI = new GuiItem(stick);
            staticPane.addItem(stickGUI, 2, x);
        }

        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 4);
        }

        ItemStack bronzeMattock = customTools.getArchToolCache(1);
        GuiItem bronzeMattockGUI = new GuiItem(bronzeMattock);
        staticPane.addItem(bronzeMattockGUI, 7, 2);

        ItemStack t1Recipe = createBronzeMattockInfo();
        GuiItem t1RecipeGUI = new GuiItem(t1Recipe);
        staticPane.addItem(t1RecipeGUI, 5, 2);

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createBronzeMattockInfo() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eUse the recipe on crafting table.");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    public void openCraftingStation(Player player, int toolID) {
        int[] craftingMaterialsArr = craftingMaterials(toolID);
        int[] componentsOwned = statistics.getComponents(player.getUniqueId());

        if (Arrays.equals(craftingMaterialsArr, new int[]{0, 0, 0, 0, 0, 0, 0})) {
            player.sendMessage("§cThere is an error processing your request.");
            return;
        }

        ChestGui gui = new ChestGui(6, configManager.craftingStationTitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 6);
        boolean canCraft = true;

        for (int x = 1; x <= 3; x++) {
            if (craftingMaterialsArr[x - 1] > 0) {
                if (componentsOwned[x - 1] >= craftingMaterialsArr[x - 1]) {
                    ItemStack itemMaterial = createItemMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, x, 1);
                } else {
                    ItemStack itemMaterial = createNoMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, x, 1);
                    canCraft = false;
                }
            }
        }

        for (int x = 4; x <= 5; x++) {
            if (craftingMaterialsArr[x - 1] > 0) {
                if (componentsOwned[x - 1] >= craftingMaterialsArr[x - 1]) {
                    ItemStack itemMaterial = createItemMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, 1, x - 2);
                } else {
                    ItemStack itemMaterial = createNoMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, 1, x - 2);
                    canCraft = false;
                }
            }
        }

        for (int x = 6; x <= 7; x++) {
            if (craftingMaterialsArr[x - 1] > 0) {
                if (componentsOwned[x - 1] >= craftingMaterialsArr[x - 1]) {
                    ItemStack itemMaterial = createItemMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, 3, x - 4);
                } else {
                    ItemStack itemMaterial = createNoMaterial(componentsOwned, craftingMaterialsArr, x - 1);
                    GuiItem item = new GuiItem(itemMaterial);
                    staticPane.addItem(item, 3, x - 4);
                    canCraft = false;
                }
            }
        }

        // required tool
        ItemStack mainHandTool = player.getInventory().getItemInMainHand();
        GuiItem tool = new GuiItem(mainHandTool);
        staticPane.addItem(tool, 2, 2);

        // upgrade tool
        ItemStack upgradeTool = customTools.getArchToolCache(toolID + 1);
        GuiItem upgrade = new GuiItem(upgradeTool);
        staticPane.addItem(upgrade, 7, 2);

        // craft button
        if (canCraft) {
            ItemStack craftButton = createCraftButton();
            staticPane.addItem(new GuiItem(craftButton, event -> processCrafting(player, craftingMaterialsArr, toolID)), 5, 2);
        } else {
            ItemStack craftButton = createUnavailableButton();
            staticPane.addItem(new GuiItem(craftButton), 5, 2);
        }

        // close button
        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 4);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private int[] craftingMaterials(int itemID) {
        return switch (itemID) {
            case 1 -> new int[]{256, 128, 0, 0, 0, 0, 0};
            case 2 -> new int[]{512, 256, 64, 32, 0, 0, 0};
            case 3 -> new int[]{768, 512, 128, 64, 16, 0, 0};
            case 4 -> new int[]{1024, 768, 256, 128, 32, 8, 0};
            case 5 -> new int[]{1280, 1024, 512, 256, 64, 16, 4};
            case 6 -> new int[]{1536, 1280, 768, 512, 128, 32, 8};
            case 7 -> new int[]{1792, 1536, 1024, 768, 256, 64, 16};
            case 8 -> new int[]{2048, 1792, 1280, 1024, 512, 128, 32};
            case 9 -> new int[]{2304, 2048, 1536, 1280, 768, 256, 64};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0};
        };
    }

    private ItemStack createItemMaterial(int[] compOwned, int[] compReq, int index) {
        ItemStack button = new ItemStack(Material.FLINT);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(customItems.getDisplayName(index + 1));
            meta.setCustomModelData(100000 + index);
            meta.setLore(List.of("§7Amount: §2" + compOwned[index] + "§7/§4" + compReq[index]));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoMaterial(int[] compOwned, int[] compReq, int index) {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(customItems.getDisplayName(index + 1));
            meta.setCustomModelData(100000);
            meta.setLore(List.of("§7Amount: §2" + compOwned[index] + "§7/§2" + compReq[index]));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processCrafting(Player player, int[] requiredMaterials, int toolID) {
        UUID uuid = player.getUniqueId();
        int[] ownedComponents = statistics.getComponents(uuid);
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        try {
            if (hasEnoughComponents(ownedComponents, requiredMaterials) && customTools.getToolId(mainHandItem) == toolID) {
                statistics.subtractComponents(uuid, requiredMaterials);
                player.getInventory().removeItem(mainHandItem);
                startCrafting(player, toolID);
            } else {
                player.sendMessage("§cYour mainhand/inventory had changed. Please try again.");
            }
        } catch (Exception e) {
            handleCraftError(player, e);
        } finally {
            player.closeInventory();
        }
    }

    private boolean hasEnoughComponents(int[] ownedComponents, int[] reqComponents) {
        for (int i = 0; i < 7; i++) {
            if (ownedComponents[i] < reqComponents[i]) {
                return false;
            }
        }
        return true;
    }

    private void handleCraftError(Player player, Exception e) {
        player.sendMessage("§cAn error occurred while processing the restoration. Please try again.");
        plugin.getLogger().severe("Error processing artefact restoration for " + player.getName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    private ItemStack createCraftButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§2Click to craft");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createUnavailableButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNot enough components to craft!");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }
}
