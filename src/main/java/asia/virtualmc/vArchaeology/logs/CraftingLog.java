package asia.virtualmc.vArchaeology.logs;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.Date;
import java.io.*;

public class CraftingLog {
    private final Main plugin;
    private final LogManager logManager;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CraftingLog(Main plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
    }

    public void logTransactionReceived(String player, String material, int amount) {
        try {
            File logs = new File(plugin.getDataFolder(), "logs");
            File salvageDir = new File(logs, "crafting-station");
            String baseFileName = LocalDate.now().format(DATE_FORMAT);
            File logFile = logManager.findAvailableLogFile(salvageDir, baseFileName);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String logEntry = String.format("[%s]: %s received %s x%d.%n",
                    timeFormat.format(new Date()),
                    player,
                    material,
                    amount);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                    writer.write(logEntry);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
                    plugin.getLogger().info(logEntry);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred during the logging process: " + e.getMessage());
        }
    }

    public void logTransactionTaken(String player, String material, int amount) {
        try {
            File logs = new File(plugin.getDataFolder(), "logs");
            File salvageDir = new File(logs, "crafting-station");
            String baseFileName = LocalDate.now().format(DATE_FORMAT);
            File logFile = logManager.findAvailableLogFile(salvageDir, baseFileName);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String logEntry = String.format("[%s]: %s x%d taken from %s.%n",
                    timeFormat.format(new Date()),
                    material,
                    amount,
                    player);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                    writer.write(logEntry);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
                    plugin.getLogger().info(logEntry);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred during the logging process: " + e.getMessage());
        }
    }
}