package asia.virtualmc.vArchaeology.items;

import de.tr7zw.changeme.nbtapi.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ItemManager {

    private final JavaPlugin plugin;
    private File customItemsFile;
    private FileConfiguration customItemsConfig;

    public ItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createCustomItemsFile();
    }

    private void createCustomItemsFile() {
        customItemsFile = new File(plugin.getDataFolder(), "custom-items.yml");
        if (!customItemsFile.exists()) {
            customItemsFile.getParentFile().mkdirs();
            plugin.saveResource("custom-items.yml", false);
        }
        customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
    }

    public ItemStack createArchItem(String itemId) {
        if (!customItemsConfig.contains("itemsList." + itemId)) {
            return null;
        }

        String path = "itemsList." + itemId;
        Material material = Material.valueOf(customItemsConfig.getString(path + ".material"));
        String name = customItemsConfig.getString(path + ".name");
        int customModelData = customItemsConfig.getInt(path + ".custom-model-data");
        List<String> lore = customItemsConfig.getStringList(path + ".lore");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(customModelData);

            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString("VARCH_ITEM", itemId);
        return nbtItem.getItem();
    }

    public void giveArchItem(UUID uuid, String itemId, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        ItemStack item = createArchItem(itemId);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + itemId);
            return;
        }

        item.setAmount(amount);
        player.getInventory().addItem(item);
    }

    public void dropArchItem(UUID uuid, String itemId) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        ItemStack item = createArchItem(itemId);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + itemId);
            return;
        }

        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

//    public void handleBlockBreak(BlockBreakEvent event, String itemId) {
//        if (event.isCancelled()) {
//            return;
//        }
//
//        dropArchItem(event.getPlayer().getUniqueId(), itemId);
//    }
}

