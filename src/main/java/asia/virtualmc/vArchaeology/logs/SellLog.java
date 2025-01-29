package asia.virtualmc.vArchaeology.logs;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.Date;
import java.io.*;

public class SellLog {
    private final Main plugin;
    private final LogManager logManager;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SellLog(Main plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
    }

    public void logTransaction(String player, String material, int value) {
        try {
            File logs = new File(plugin.getDataFolder(), "logs");
            File salvageDir = new File(logs, "sell-gui");
            String baseFileName = LocalDate.now().format(DATE_FORMAT);
            File logFile = logManager.findAvailableLogFile(salvageDir, baseFileName);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String logEntry = String.format("[%s]: %s sold %d %s materials.%n",
                    timeFormat.format(new Date()),
                    player,
                    value,
                    material);

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