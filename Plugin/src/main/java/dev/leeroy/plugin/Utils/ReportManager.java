package dev.leeroy.plugin.Utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportManager {

    private final JavaPlugin plugin;
    private final File reportFile;
    private YamlConfiguration config;

    public ReportManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reportFile = new File(plugin.getDataFolder(), "reports.yml");
        load();
    }

    private void load() {
        if (!reportFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { reportFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(reportFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(reportFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    /**
     * Adds a new report. Returns the report ID.
     */
    public String addReport(String reporterName, String targetName, String reason) {
        String id = String.valueOf(System.currentTimeMillis());
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        config.set(id + ".reporter", reporterName);
        config.set(id + ".target",   targetName);
        config.set(id + ".reason",   reason);
        config.set(id + ".time",     timestamp);
        config.set(id + ".resolved", false);
        save();
        return id;
    }

    /**
     * Deletes a report by ID.
     */
    public boolean deleteReport(String id) {
        if (!config.contains(id)) return false;
        config.set(id, null);
        save();
        return true;
    }

    /**
     * Returns all unresolved reports as a list of maps.
     */
    public List<Map<String, String>> getReports() {
        List<Map<String, String>> reports = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            Map<String, String> report = new LinkedHashMap<>();
            report.put("id",       key);
            report.put("reporter", config.getString(key + ".reporter"));
            report.put("target",   config.getString(key + ".target"));
            report.put("reason",   config.getString(key + ".reason"));
            report.put("time",     config.getString(key + ".time"));
            reports.add(report);
        }
        return reports;
    }

    public boolean reportExists(String id) {
        return config.contains(id);
    }
}