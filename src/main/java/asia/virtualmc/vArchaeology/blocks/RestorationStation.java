package asia.virtualmc.vArchaeology.blocks;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.items.ArtefactCollections;
import asia.virtualmc.vArchaeology.items.ArtefactItems;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.*;

public class RestorationStation implements Listener {

    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final EXPManager expManager;
    private final PlayerData playerData;
    private final Statistics statistics;
    private final ArtefactItems artefactItems;
    private final ConfigManager configManager;
    private final ArtefactCollections artefactCollections;
    private final NamespacedKey ARTEFACT_KEY;
    private Location restorationStationLocation;
    private final Map<UUID, Hologram> activeHolograms;
    private final Map<UUID, BukkitRunnable> activeCraftingTasks;
    private final Map<UUID, Long> cooldowns;
    private final Random random;

    public RestorationStation(Main plugin,
                              EffectsUtil effectsUtil,
                              EXPManager expManager,
                              PlayerData playerData,
                              Statistics statistics,
                              ArtefactItems artefactItems,
                              ConfigManager configManager,
                              ArtefactCollections artefactCollections) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.expManager = expManager;
        this.playerData = playerData;
        this.statistics = statistics;
        this.artefactItems = artefactItems;
        this.configManager = configManager;
        this.artefactCollections = artefactCollections;
        this.activeHolograms = new HashMap<>();
        this.activeCraftingTasks = new HashMap<>();
        this.cooldowns = new HashMap<>();
        this.random = new Random();
        this.ARTEFACT_KEY = new NamespacedKey(plugin, "varch_artefact");

        loadRestorationStation();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadRestorationStation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("settings.interactableBlocks.restoration-station")) {
            String[] coords = config.getString("settings.interactableBlocks.restoration-station").split(",");
            try {
                double x = Double.parseDouble(coords[0].trim());
                double y = Double.parseDouble(coords[1].trim());
                double z = Double.parseDouble(coords[2].trim());
                this.restorationStationLocation = new Location(plugin.getServer().getWorlds().get(0), x, y, z);

                if (x == 0 && y == 0 && z == 0) {
                    plugin.getLogger().severe("Restoration station coordinates are (0, 0, 0). Block will not be created.");
                    restorationStationLocation = null;
                } else {
                    ensureBlockExists();
                }

            } catch (NumberFormatException e) {
                plugin.getLogger().severe("Invalid restoration station coordinates in config.yml");
            }
        } else {
            plugin.getLogger().severe("No restoration station location found in config.yml.");
            restorationStationLocation = null;
        }
    }

    private void ensureBlockExists() {
        if (restorationStationLocation != null) {
            Block block = restorationStationLocation.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.STONE);
                plugin.getLogger().info("Created restoration station block at " + formatLocation(restorationStationLocation));
            }
        }
    }

    public void createRestorationStation(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null) {
            Location blockLocation = targetBlock.getLocation();
            FileConfiguration config = plugin.getConfig();
            String locationString = String.format("%d, %d, %d",
                    blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
            config.set("settings.interactableBlocks.restoration-station", locationString);
            plugin.saveConfig();

            plugin.getLogger().info("Saved new restoration station location: " + locationString);
            loadRestorationStation();
        } else {
            player.sendMessage("§cNo block in view to set as a restoration station!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (restorationStationLocation != null && clickedBlock.getLocation().equals(restorationStationLocation)) {
            event.setCancelled(true);

            long currentTime = System.currentTimeMillis();
            if (cooldowns.containsKey(playerUUID)) {
                long lastInteraction = cooldowns.get(playerUUID);
                if (currentTime - lastInteraction < 1000) {
                    return;
                }
            }
            if (activeCraftingTasks.containsKey(playerUUID)) {
                player.sendMessage("§cYou are currently restoring an artefact!");
                return;
            }
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (mainHandItem.getType() == Material.AIR) {
                return;
            }

            if (!mainHandItem.hasItemMeta() ||
                    !mainHandItem.getItemMeta().getPersistentDataContainer().has(ARTEFACT_KEY, PersistentDataType.INTEGER)) {
                return;
            }
            cooldowns.put(playerUUID, currentTime);
            openRestoreArtefact(player);
        }
    }

    private void startCrafting(Player player, double exp, boolean isType2Crafting, int collectionID) {
        Location holoLocation = restorationStationLocation.clone().add(0.5, 1.5, 0.5);
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

            activeHolograms.put(player.getUniqueId(), hologram);
            effectsUtil.playSoundUUID(uuid, "minecraft:cozyvanilla.restoration_sounds", Sound.Source.PLAYER, 1.0f, 1.0f);

            BukkitRunnable craftingTask = new BukkitRunnable() {
                private int secondsPassed = 0;

                @Override
                public void run() {
                    secondsPassed++;

                    if (secondsPassed <= 8) {
                        DHAPI.setHologramLine(hologram, 0, progressChars[secondsPassed - 1]);

                    }
                    else if (secondsPassed == 9 && isType2Crafting) {
                        DHAPI.removeHologramLine(hologram, 0);
                        String hologramItem = "#ICON: FLINT {CustomModelData:" + artefactCollections.getCollectionModelData(collectionID) + "}";
                        DHAPI.addHologramLine(hologram, hologramItem);
                    }
                    else {
                        DHAPI.removeHologram(holoName);
                        activeHolograms.remove(player.getUniqueId());
                        String formattedEXP = String.format("%.2f", exp);

                        effectsUtil.spawnFireworks(uuid, 3, 3);
                        effectsUtil.sendTitleMessage(uuid, "", "<#7CFEA7>You have received <gold>" +
                                formattedEXP + " <#7CFEA7>XP!");
                        expManager.addRestorationXP(uuid, exp);
                        if (isType2Crafting) artefactCollections.giveCollection(uuid, collectionID, 1);

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
    public void openRestoreArtefact(Player player) {
        UUID uuid = player.getUniqueId();
        double initialXP = expManager.getTotalArtefactRestoreEXP(uuid);
        int archLevel = playerData.getArchLevel(uuid);

        ChestGui gui = new ChestGui(4, configManager.arteRestoreGUITitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 4);

        // type-1 restoration (Lv. 30)
        if (archLevel >= 30) {
            for (int x = 1; x <= 3; x++) {
                ItemStack confirmButton = createType1Button(initialXP * 0.30);
                GuiItem guiItem = new GuiItem(confirmButton, event -> openRestoreT1ArtefactConfirm(player, initialXP * 0.30));
                staticPane.addItem(guiItem, x, 1);
            }
        } else {
            for (int x = 1; x <= 3; x++) {
                ItemStack confirmButton = createNoAccessType1();
                GuiItem guiItem = new GuiItem(confirmButton);
                staticPane.addItem(guiItem, x, 1);
            }
        }

        // type-2 restoration (Lv. 60)
        if (archLevel >= 60) {
            ArrayList<Integer> componentsOwned = new ArrayList<>(statistics.getComponents(uuid));
            for (int x = 5; x <= 7; x++) {
                ItemStack confirmButton = createType2Button(initialXP, componentsOwned);
                GuiItem guiItem = new GuiItem(confirmButton, event -> openRestoreT2ArtefactConfirm(player, initialXP));
                staticPane.addItem(guiItem, x, 1);
            }
        } else {
            for (int x = 5; x <= 7; x++) {
                ItemStack confirmButton = createNoAccessType2();
                GuiItem guiItem = new GuiItem(confirmButton);
                staticPane.addItem(guiItem, x, 1);
            }
        }

        // close button
        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 3);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private void openRestoreT1ArtefactConfirm(Player player, double initialXP) {
        ChestGui gui = new ChestGui(3, configManager.confirmGUITitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        // type-1 restoration (Lv. 30)
        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createConfirmationButton();
            GuiItem guiItem = new GuiItem(confirmButton, event -> processType1Restore(player));
            staticPane.addItem(guiItem, x, 1);
        }

        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private void openRestoreT2ArtefactConfirm(Player player, double initialXP) {
        ChestGui gui = new ChestGui(3, configManager.confirmGUITitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        // type-2 restoration (Lv. 60)
        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createConfirmationButton();
            GuiItem guiItem = new GuiItem(confirmButton, event -> processType2Restore(player));
            staticPane.addItem(guiItem, x, 1);
        }

        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createType1Button(double exp) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eType-1 Restoration §7(§a+" + formattedXP + " XP§7)");
            meta.setCustomModelData(configManager.invisibleModelData);
            meta.setLore(List.of(
                    "§cDOES NOT REQUIRE §7components to use but it",
                    "§7will not produce anything.",
                    "",
                    "§4§lWARNING! §cThis process completely destroy",
                    "§cyour artefact."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createType2Button(double exp, ArrayList<Integer> componentsOwned) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eType-2 Restoration §7(§a+" + formattedXP + " XP§7)");
            meta.setCustomModelData(configManager.invisibleModelData);
            meta.setLore(List.of(
                    "§7This require the following components",
                    "§7to ensure a successful restoration process.",
                    "",
                    "§7• §aCommon Components: §2" + componentsOwned.get(0) + "§7/§c64",
                    "§7• §bUncommon Components: §2" + componentsOwned.get(1) + "§7/§c32",
                    "§7• §3Rare Components: §2" + componentsOwned.get(2) + "§7/§c16",
                    "§7• §eUnique Components: §2" + componentsOwned.get(3) + "§7/§c12",
                    "§7• §6Special Components: §2" + componentsOwned.get(4) + "§7/§c8",
                    "§7• §5Mythical Components: §2" + componentsOwned.get(5) + "§7/§c4",
                    "§7• §4Exotic Components: §2" + componentsOwned.get(6) + "§7/§c2"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processType1Restore(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();
        double finalXP = expManager.getTotalArtefactRestoreEXP(uuid) * 0.30;

        if (cannotProcessRestoration(player, finalXP)) {
            return;
        }

        try {
            player.getInventory().removeItem(item);
            startCrafting(player, finalXP, false, artefactCollections.getRandomCollection(artefactItems.getArtefactID(item)));
        } catch (Exception e) {
            handleRestoreError(player, e);
        } finally {
            player.closeInventory();
        }
    }

    private void processType2Restore(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();
        double finalXP = expManager.getTotalArtefactRestoreEXP(uuid);

        if (cannotProcessRestoration(player, finalXP)) {
            return;
        }

        if (!validateComponents(player, uuid)) {
            return;
        }

        try {
            player.getInventory().removeItem(item);
            List<Integer> componentsRequired = Arrays.asList(64, 32, 16, 12, 8, 4, 2);
            statistics.subtractComponents(uuid, componentsRequired);
            startCrafting(player, finalXP, true, artefactCollections.getRandomCollection(artefactItems.getArtefactID(item)));
        } catch (Exception e) {
            handleRestoreError(player, e);
        } finally {
            player.closeInventory();
        }
    }

    private boolean cannotProcessRestoration(Player player, double finalXP) {
        ItemStack itemMainHand = player.getInventory().getItemInMainHand();
        int artefactID = artefactItems.getArtefactID(itemMainHand);

        if (artefactID == 0) {
            player.sendMessage("§cError: Artefact not found in your main hand. Please try again.");
            player.closeInventory();
            return true;
        }

        if (finalXP <= 0) {
            player.sendMessage("§cThere was an error processing the action. Please contact the administrator.");
            player.closeInventory();
            return true;
        }

        return false;
    }

    private boolean validateComponents(Player player, UUID uuid) {
        ArrayList<Integer> componentsOwned = new ArrayList<>(statistics.getComponents(uuid));
        List<Integer> componentsRequired = Arrays.asList(64, 32, 16, 12, 8, 4, 2);

        if (componentsOwned.size() != componentsRequired.size() ||
                !hasRequiredComponents(componentsOwned, componentsRequired)) {
            player.sendMessage("§cError: You do not have the required number of components to do this.");
            player.closeInventory();
            return false;
        }
        return true;
    }

    private boolean hasRequiredComponents(List<Integer> owned, List<Integer> required) {
        for (int i = 0; i < owned.size(); i++) {
            if (owned.get(i) < required.get(i)) {
                return false;
            }
        }
        return true;
    }

    private void handleRestoreError(Player player, Exception e) {
        player.sendMessage("§cAn error occurred while processing the restoration. Please try again.");
        plugin.getLogger().severe("Error processing artefact restoration for " + player.getName() + ": " + e.getMessage());
        e.printStackTrace();
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

    private ItemStack createConfirmationButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aConfirm process.");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoAccessType1() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cUnlocked at Lv. 30");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoAccessType2() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cUnlocked at Lv. 60");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }
}