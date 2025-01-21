package asia.virtualmc.vArchaeology.logs;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;

import java.util.zip.GZIPOutputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.List;

public class LogManager {
    private final Main plugin;
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1 MB in bytes
    private static final int COMPRESSION_DAYS = 7;
    private static final int DELETION_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MAINTENANCE_LOG_FILE = "maintenance-logs.txt";

    public LogManager(Main plugin) {
        this.plugin = plugin;
        createDirectories();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkAndPerformMaintenance);
    }

    private void checkAndPerformMaintenance() {
        try {
            File maintenanceLog = new File(new File(plugin.getDataFolder(), "logs"), MAINTENANCE_LOG_FILE);
            LocalDate today = LocalDate.now();

            // Create maintenance log file if it doesn't exist
            if (!maintenanceLog.exists()) {
                maintenanceLog.createNewFile();
                performMaintenanceAndLog();
                return;
            }

            // Read the last maintenance date
            String lastLine = readLastLine(maintenanceLog);
            if (lastLine == null || !wasMaintenancePerformedToday(lastLine, today)) {
                performMaintenanceAndLog();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error checking maintenance log: " + e.getMessage());
        }
    }

    private boolean wasMaintenancePerformedToday(String lastLine, LocalDate today) {
        try {
            // Extract date from log line format "[yyyy-MM-dd]: Successfully performed maintenance on log files."
            String dateStr = lastLine.substring(1, 11);
            LocalDate lastMaintenanceDate = LocalDate.parse(dateStr, DATE_FORMAT);
            return lastMaintenanceDate.equals(today);
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing last maintenance date: " + e.getMessage());
            return false;
        }
    }

    private void performMaintenanceAndLog() {
        // Perform maintenance for each directory
        String[] directories = {"salvage-station", "sell-gui", "crafting-station"};
        for (String dir : directories) {
            performLogMaintenance(dir);
        }

        // Log the maintenance execution
        try {
            File maintenanceLog = new File(new File(plugin.getDataFolder(), "logs"), MAINTENANCE_LOG_FILE);
            String logEntry = String.format("[%s]: Successfully performed maintenance on log files.%n",
                    LocalDate.now().format(DATE_FORMAT));

            Files.write(maintenanceLog.toPath(), logEntry.getBytes(),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            plugin.getLogger().severe("Error writing to maintenance log: " + e.getMessage());
        }
    }

    private String readLastLine(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (!lines.isEmpty()) {
                return lines.get(lines.size() - 1);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error reading maintenance log: " + e.getMessage());
        }
        return null;
    }

    private void createDirectories() {
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        String[] dir = {
                "salvage-station",
                "sell-gui",
                "crafting-station"
        };
        for (int i = 0; i < dir.length; i++) {
            File logDir = new File(logsDir, dir[i]);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
        }
    }

    public File findAvailableLogFile(File directory, String baseFileName) {
        File logFile = new File(directory, baseFileName + ".txt");
        int counter = 2;

        while (logFile.exists() && logFile.length() >= MAX_FILE_SIZE) {
            logFile = new File(directory, String.format("%s-%d.txt", baseFileName, counter));
            counter++;
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create new log file: " + e.getMessage());
            }
        }
        return logFile;
    }

    private void performLogMaintenance(String dirName) {
        try {
            File directory = new File(new File(plugin.getDataFolder(), "logs"), dirName);
            if (!directory.exists()) return;

            LocalDate today = LocalDate.now();

            for (File file : directory.listFiles()) {
                if (!file.isFile()) continue;

                String fileName = file.getName();
                if (!fileName.endsWith(".txt") && !fileName.endsWith(".gz")) continue;

                String dateStr = fileName.split("-\\d+\\.")[0].split("\\.")[0];
                LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMAT);
                long daysOld = ChronoUnit.DAYS.between(fileDate, today);

                if (daysOld >= DELETION_DAYS) {
                    Files.deleteIfExists(file.toPath());
                    plugin.getLogger().info("Deleted old log file: " + fileName);
                } else if (daysOld >= COMPRESSION_DAYS && fileName.endsWith(".txt")) {
                    compressFile(file);
                    plugin.getLogger().info("Compressed log file: " + fileName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error during log maintenance: " + e.getMessage());
        }
    }

    private void compressFile(File file) throws IOException {
        String gzFileName = file.getPath().replace(".txt", ".gz");
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(gzFileName));
             FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, len);
            }
        }
        Files.deleteIfExists(file.toPath());
    }
}