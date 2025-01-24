package asia.virtualmc.vArchaeology.utilities;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.metadata.FixedMetadataValue;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.minimessage.MiniMessage;

import me.clip.placeholderapi.PlaceholderAPI;

import java.util.UUID;

public class EffectsUtil {

    private final Main plugin;

    public EffectsUtil(Main plugin) {
        this.plugin = plugin;
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
                meta.addEffect(effect);
                meta.setPower(0); // Minimize travel
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

    public void sendADBProgressBarTitle(UUID playerUUID, double adbProgress, double adbAdd) {
        if (adbProgress < 0.0 || adbProgress > 1.0) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0");
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }

        int totalBars = 30;
        int filledBars = (int) Math.round(adbProgress * totalBars);
        int emptyBars = totalBars - filledBars;

        StringBuilder barBuilder = new StringBuilder();
        barBuilder.append("§2");
        barBuilder.append("❙".repeat(filledBars));
        barBuilder.append("§8");
        barBuilder.append("❙".repeat(emptyBars));
        barBuilder
                .append(" §e(")
                .append(String.format("%.2f", adbProgress * 100.0))
                .append("%)");
        barBuilder.append(" §7| ");
        barBuilder
                .append("§a(")
                .append(String.format("%.2f", adbAdd))
                .append("%)");

        String progressBarMiniMsg = barBuilder.toString();
        Component title = MiniMessage.miniMessage().deserialize("<gradient:#EBD197:#B48811>Artefact Discovery</gradient>");
        Component subtitle = MiniMessage.miniMessage().deserialize(progressBarMiniMsg);

        Title fullTitle = Title.title(title, subtitle);
        player.showTitle(fullTitle);
    }
}