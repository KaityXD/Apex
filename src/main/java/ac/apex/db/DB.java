package ac.apex.db;

import ac.apex.punish.Ban;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DB {
    private static DB inst;
    private String url;
    private String user;
    private String pass;
    private boolean sqlite;

    private DB() {}

    public static DB i() {
        if (inst == null) inst = new DB();
        return inst;
    }

    public void init(Plugin pl) {
        close();
        String type = pl.getConfig().getString("db.type", "sqlite");
        sqlite = type == null || type.equalsIgnoreCase("sqlite");
        if (sqlite) {
            String file = pl.getConfig().getString("db.file", "data.db");
            File f = new File(pl.getDataFolder(), file);
            f.getParentFile().mkdirs();
            url = "jdbc:sqlite:" + f.getAbsolutePath();
            user = "";
            pass = "";
            try { Class.forName("org.sqlite.JDBC"); } catch (Throwable ignored) {}
        } else {
            String host = pl.getConfig().getString("db.host", "localhost");
            int port = pl.getConfig().getInt("db.port", 3306);
            String name = pl.getConfig().getString("db.name", "apex");
            user = pl.getConfig().getString("db.user", "root");
            pass = pl.getConfig().getString("db.pass", "");
            url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Throwable ignored) {}
        }
        mk();
    }

    private void mk() {
        String sql = "CREATE TABLE IF NOT EXISTS bans (" +
                "id TEXT PRIMARY KEY," +
                "uuid TEXT NOT NULL," +
                "name TEXT," +
                "reason TEXT," +
                "checkName TEXT," +
                "time BIGINT," +
                "expiry BIGINT)";
        if (!sqlite) sql = sql.replace("TEXT PRIMARY KEY", "VARCHAR(32) PRIMARY KEY").replace("TEXT NOT NULL", "VARCHAR(36) NOT NULL");
        try (Connection c = con(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection con() throws SQLException {
        if (url == null) throw new SQLException("db not init");
        if (sqlite) return DriverManager.getConnection(url);
        return DriverManager.getConnection(url, user, pass);
    }

    public void add(Ban b) {
        String sql = sqlite ?
                "INSERT OR REPLACE INTO bans(id,uuid,name,reason,checkName,time,expiry) VALUES(?,?,?,?,?,?,?)" :
                "REPLACE INTO bans(id,uuid,name,reason,checkName,time,expiry) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.id());
            ps.setString(2, b.uuid().toString());
            ps.setString(3, b.name());
            ps.setString(4, b.reason());
            ps.setString(5, b.check());
            ps.setLong(6, b.time());
            ps.setLong(7, b.expiry());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean del(UUID uuid) {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("DELETE FROM bans WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean del(String name) {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("DELETE FROM bans WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Ban get(UUID uuid) {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bans WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<UUID, Ban> all() {
        Map<UUID, Ban> m = new ConcurrentHashMap<>();
        try (Connection c = con(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM bans")) {
            while (rs.next()) {
                Ban b = map(rs);
                m.put(b.uuid(), b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private Ban map(ResultSet rs) throws SQLException {
        return new Ban(
                rs.getString("id"),
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getString("reason"),
                rs.getString("checkName"),
                rs.getLong("time"),
                rs.getLong("expiry")
        );
    }

    public void close() {
        url = null;
        user = null;
        pass = null;
    }
}
