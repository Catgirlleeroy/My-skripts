package dev.leeroy.plugin.Utils.misc;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ReportManager {

    private final DatabaseManager db;

    public ReportManager(DatabaseManager db) {
        this.db = db;
    }

    public String addReport(String reporterName, String targetName, String reason) {
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        String sql = "INSERT INTO reports (reporter, target, reason, time) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reporterName);
            ps.setString(2, targetName);
            ps.setString(3, reason);
            ps.setString(4, time);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return String.valueOf(rs.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.valueOf(System.currentTimeMillis());
    }

    public boolean deleteReport(String id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE id = ?")) {
            ps.setLong(1, Long.parseLong(id));
            return ps.executeUpdate() > 0;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, String>> getReports() {
        List<Map<String, String>> reports = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, reporter, target, reason, time FROM reports ORDER BY id ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> report = new LinkedHashMap<>();
                report.put("id",       String.valueOf(rs.getLong("id")));
                report.put("reporter", rs.getString("reporter"));
                report.put("target",   rs.getString("target"));
                report.put("reason",   rs.getString("reason"));
                report.put("time",     rs.getString("time"));
                reports.add(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }
}
