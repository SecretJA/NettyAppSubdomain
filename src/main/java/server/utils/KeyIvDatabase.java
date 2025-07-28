package server.utils;

import server.model.KeyIvRecord;
import java.sql.*;
import java.util.*;

/**
 * Lưu trữ AES key và IV vào SQLite database.
 */
public class KeyIvDatabase {
    private static final String DB_URL = "jdbc:sqlite:keyiv_db.sqlite";

    static {
        // Tạo bảng nếu chưa có
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS keyiv (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    aes_key TEXT NOT NULL,
                    aes_iv TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    target_domain TEXT,
                    client_ip TEXT
                )
                """;
            conn.createStatement().execute(sql);
            
            // Tạo index cho timestamp để query nhanh hơn
            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON keyiv(timestamp)");
            conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_client_ip ON keyiv(client_ip)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void save(KeyIvRecord record) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO keyiv (aes_key, aes_iv, target_domain, client_ip) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, record.aesKey);
            ps.setString(2, record.aesIv);
            ps.setString(3, record.targetDomain);
            ps.setString(4, record.clientIp);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<KeyIvRecord> loadAll() {
        List<KeyIvRecord> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT aes_key, aes_iv, timestamp, target_domain, client_ip FROM keyiv ORDER BY timestamp DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                list.add(new KeyIvRecord(
                    rs.getString(1), // aes_key
                    rs.getString(2), // aes_iv
                    rs.getString(3), // timestamp
                    rs.getString(4), // target_domain
                    rs.getString(5)  // client_ip
                ));
            }
        } catch (SQLException ignored) {}
        return list;
    }

    public static int getRecordCount() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT COUNT(*) FROM keyiv";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public static long getDatabaseSize() {
        try {
            java.io.File dbFile = new java.io.File("keyiv_db.sqlite");
            return dbFile.length();
        } catch (Exception e) {
            return 0;
        }
    }

    public static void cleanupOldRecords(int daysToKeep) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "DELETE FROM keyiv WHERE timestamp < datetime('now', '-" + daysToKeep + " days')";
            int deleted = conn.createStatement().executeUpdate(sql);
            System.out.println("🧹 Đã xóa " + deleted + " records cũ");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                SELECT 
                    COUNT(*) as total_records,
                    COUNT(DISTINCT client_ip) as unique_clients,
                    COUNT(DISTINCT target_domain) as unique_domains,
                    MIN(timestamp) as first_record,
                    MAX(timestamp) as last_record
                FROM keyiv
                """;
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                stats.put("total_records", rs.getInt("total_records"));
                stats.put("unique_clients", rs.getInt("unique_clients"));
                stats.put("unique_domains", rs.getInt("unique_domains"));
                stats.put("first_record", rs.getString("first_record"));
                stats.put("last_record", rs.getString("last_record"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
} 