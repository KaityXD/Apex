package ac.apex.db;

import ac.apex.punish.Ban;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DB {
    private static DB inst;
    private String url;
    private String user;
    private String pass;
    private boolean sqlite;
    private boolean fallback;
    private File file;
    private final Gson gson = new Gson();

    private DB() {}

    public static DB i() {
        if (inst == null) inst = new DB();
        return inst;
    }

    public void init(Plugin pl) {
        close();
        String type = pl.getConfig().getString("db.type", "sqlite");
        sqlite = type == null || type.equalsIgnoreCase("sqlite");
        fallback = false;
        file = new File(pl.getDataFolder(), "data/bans.json");
        if (sqlite) {
            String fn = pl.getConfig().getString("db.file", "data.db");
            File f = new File(pl.getDataFolder(), fn);
            f.getParentFile().mkdirs();
            url = "jdbc:sqlite:" + f.getAbsolutePath();
            user = "";
            pass = "";
            try {
                Class.forName("org.sqlite.JDBC");
                try (Connection c = DriverManager.getConnection(url)) { c.close(); }
            } catch (Throwable e) {
                fallback = true;
                url = null;
            }
        } else {
            String host = pl.getConfig().getString("db.host", "localhost");
            int port = pl.getConfig().getInt("db.port", 3306);
            String name = pl.getConfig().getString("db.name", "apex");
            user = pl.getConfig().getString("db.user", "root");
            pass = pl.getConfig().getString("db.pass", "");
            url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection c = DriverManager.getConnection(url, user, pass)) { c.close(); }
            } catch (Throwable e) {
                fallback = true;
                url = null;
            }
        }
        if (!fallback) mk();
    }

    private void mk() {
        if (fallback) return;
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
            fallback = true;
        }
    }

    public Connection con() throws SQLException {
        if (fallback || url == null) throw new SQLException("db fallback");
        if (sqlite) return DriverManager.getConnection(url);
        return DriverManager.getConnection(url, user, pass);
    }

    public void add(Ban b) {
        if (fallback) { fbAdd(b); return; }
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
            fbAdd(b);
        }
    }

    public boolean del(UUID uuid) {
        if (fallback) return fbDel(uuid);
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("DELETE FROM bans WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return fbDel(uuid);
        }
    }

    public boolean del(String name) {
        if (fallback) return fbDel(name);
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("DELETE FROM bans WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return fbDel(name);
        }
    }

    public Ban get(UUID uuid) {
        if (fallback) return fbGet(uuid);
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bans WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            return fbGet(uuid);
        }
        return null;
    }

    public Map<UUID, Ban> all() {
        if (fallback) return fbAll();
        Map<UUID, Ban> m = new ConcurrentHashMap<>();
        try (Connection c = con(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM bans")) {
            while (rs.next()) {
                Ban b = map(rs);
                m.put(b.uuid(), b);
            }
        } catch (SQLException e) {
            return fbAll();
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

    private synchronized void fbAdd(Ban b) {
        Map<UUID, Ban> m = fbAll();
        m.put(b.uuid(), b);
        fbSave(m);
    }

    private synchronized boolean fbDel(UUID uuid) {
        Map<UUID, Ban> m = fbAll();
        if (m.remove(uuid) != null) { fbSave(m); return true; }
        return false;
    }

    private synchronized boolean fbDel(String name) {
        Map<UUID, Ban> m = fbAll();
        boolean r = false;
        Iterator<Map.Entry<UUID, Ban>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().name().equalsIgnoreCase(name)) { it.remove(); r = true; }
        }
        if (r) fbSave(m);
        return r;
    }

    private synchronized Ban fbGet(UUID uuid) { return fbAll().get(uuid); }

    private synchronized Map<UUID, Ban> fbAll() {
        if (!file.exists()) return new ConcurrentHashMap<>();
        try (FileReader r = new FileReader(file)) {
            Type t = new TypeToken<Map<UUID, Ban>>() {}.getType();
            Map<UUID, Ban> d = gson.fromJson(r, t);
            if (d != null) return new ConcurrentHashMap<>(d);
        } catch (Exception ignored) {}
        return new ConcurrentHashMap<>();
    }

    private synchronized void fbSave(Map<UUID, Ban> m) {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) { gson.toJson(m, w); }
        } catch (Exception ignored) {}
    }

    public void close() {
        url = null;
        user = null;
        pass = null;
        fallback = false;
    }

    public boolean isFallback() { return fallback; }
}
