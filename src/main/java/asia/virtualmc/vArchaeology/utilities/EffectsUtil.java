package asia.virtualmc.vArchaeology.utilities;

import asia.virtualmc.vArchaeology.Main;

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.metadata.FixedMetadataValue;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.minimessage.MiniMessage;

import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;

import java.util.UUID;

public class EffectsUtil {

    private final Main plugin;
    private UltimateAdvancementAPI uaapi;

    public EffectsUtil(Main plugin) {
        this.plugin = plugin;
        this.uaapi = UltimateAdvancementAPI.getInstance(plugin);
    }

    public void spawnFireworks(UUID uuid, int amount, long interval) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        Location location = player.getLocation();

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= amount) {
                    this.cancel();
                    return;
                }
                Firework firework = world.spawn(location, Firework.class);
                firework.setMetadata("nodamage", new FixedMetadataValue(plugin, true));
                FireworkMeta meta = firework.getFireworkMeta();

                FireworkEffect effect = FireworkEffect.builder()
                        .withColor(Color.AQUA, Color.LIME)
                        .withFade(Color.YELLOW)
                        .with(FireworkEffect.Type.BALL)
                        .trail(true)
                        .flicker(true)
                        .build();
                meta.setPower(0);
                meta.addEffect(effect);
                firework.setFireworkMeta(meta);
                count++;
            }
        }.runTaskTimer(plugin, 0, interval);
    }

    public void playSoundUUID(UUID playerUUID, String soundKey, Sound.Source source, float volume, float pitch) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }
        String[] parts = soundKey.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String key = parts.length > 1 ? parts[1] : parts[0];

        Sound sound = Sound.sound()
                .type(Key.key(namespace, key))
                .source(source)
                .volume(volume)
                .pitch(pitch)
                .build();
        player.playSound(sound);
    }

    public void playSound(Player player, String soundKey, Sound.Source source, float volume, float pitch) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String[] parts = soundKey.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String key = parts.length > 1 ? parts[1] : parts[0];

        Sound sound = Sound.sound()
                .type(Key.key(namespace, key))
                .source(source)
                .volume(volume)
                .pitch(pitch)
                .build();
        player.playSound(sound);
    }

    public void sendTitleMessage(UUID uuid, String title, String subtitle) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        Component titleComponent = MiniMessage.miniMessage().deserialize(title);
        Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitle);

        Title fullTitle = Title.title(titleComponent, subtitleComponent);
        player.showTitle(fullTitle);
    }

    public void sendPlayerMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        Component messageComponent = MiniMessage.miniMessage().deserialize(message);
        player.sendMessage(messageComponent);
    }

    public void sendCustomToast(UUID uuid, int modelData) {
        ItemStack icon = new ItemStack(Material.FLINT);
        Player player = Bukkit.getPlayer(uuid);

        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            icon.setItemMeta(meta);
        }
        uaapi.displayCustomToast(player, icon, "Collection Log Updated", AdvancementFrameType.GOAL);
        playSound(player, "minecraft:cozyvanilla.collection_log_updated", Sound.Source.PLAYER, 1.0f, 1.0f);
    }

    public void sendADBProgressBarTitle(UUID playerUUID, double adbProgress, double adbAdd) {
        if (adbProgress < 0.0 || adbProgress > 1.0) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0");
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }

        int totalBars = 25;
        int filledBars = (int) Math.round(adbProgress * totalBars);
        int emptyBars = totalBars - filledBars;

        StringBuilder progressBar = new StringBuilder();
        progressBar.append("<dark_green>");
        progressBar.append("❙".repeat(filledBars));
        progressBar.append("<dark_gray>");
        progressBar.append("❙".repeat(emptyBars));

        StringBuilder subtitleString = new StringBuilder();
        subtitleString.append("<gradient:#EBD197:#B48811>Artefact Discovery: ");
        subtitleString
                .append(String.format("%.2f", adbProgress * 100.0))
                .append("%")
                .append("</gradient>");
        subtitleString
                .append(" <green>(+")
                .append(String.format("%.2f", adbAdd))
                .append("%)");

        Component title = MiniMessage.miniMessage().deserialize(progressBar.toString());
        Component subtitle = MiniMessage.miniMessage().deserialize(subtitleString.toString());

        Title fullTitle = Title.title(title, subtitle);
        player.showTitle(fullTitle);
    }
}