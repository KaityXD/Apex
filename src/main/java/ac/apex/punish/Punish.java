package ac.apex.punish;

import ac.apex.Apex;
import ac.apex.compat.Platform;
import ac.apex.util.Chat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Punish implements Listener {
    private final Apex plugin;
    private final Map<UUID, Ban> bans = new ConcurrentHashMap<>();
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private volatile List<String> cachedScreen = null;
    private volatile String cachedAppeal = "https://discord.gg/your-server";
    private volatile String cachedBroadcastFmt = "&8[&b&lAPEX&8] &b%player% &7was removed by Apex for &c%reason% &8(&f%check%&8)";
    private volatile boolean cachedBroadcastEnabled = true;

    public Punish(Apex plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/bans.json");
        load();
        cacheConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void cacheConfig() {
        try {
            List<String> lines = plugin.getConfig().getStringList("punishments.ban-screen");
            if (lines != null && !lines.isEmpty()) cachedScreen = new ArrayList<>(lines);
            String appeal = plugin.getConfig().getString("punishments.appeal-url");
            if (appeal != null && !appeal.isEmpty()) cachedAppeal = appeal;
            String fmt = plugin.getConfig().getString("punishments.broadcast.format");
            if (fmt != null && !fmt.isEmpty()) cachedBroadcastFmt = fmt;
            cachedBroadcastEnabled = plugin.getConfig().getBoolean("punishments.broadcast.enabled", true);
        } catch (Throwable ignored) {}
    }

    public void reloadCache() { cacheConfig(); }

    public void execute(Player p, String reason, String check, String time) {
        long dur = parse(time);
        long now = System.currentTimeMillis();
        long exp = (dur <= 0) ? -1 : now + dur;
        String id = "#APX-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Ban ban = new Ban(id, p.getUniqueId(), p.getName(), reason, check, now, exp);
        bans.put(p.getUniqueId(), ban);
        save();

        String scr = screen(ban);
        Platform.entity(plugin, p, e -> ((Player) e).kickPlayer(scr));
        broadcast(ban);
    }

    public boolean unban(String name) {
        for (Map.Entry<UUID, Ban> e : bans.entrySet()) {
            if (e.getValue().name().equalsIgnoreCase(name)) {
                bans.remove(e.getKey());
                save();
                return true;
            }
        }
        return false;
    }

    public String screen(Ban b) {
        List<String> lines = cachedScreen;
        if (lines == null || lines.isEmpty()) {
            try { lines = plugin.getConfig().getStringList("punishments.ban-screen"); } catch (Throwable ignored) {}
            if (lines == null || lines.isEmpty()) {
                lines = Arrays.asList("&c&lSUSPENDED BY APEX", "&7Reason: &f%reason%", "&7Expires: &e%expires%");
            }
        }

        String expStr = b.perm() ? "Permanent" : fmt(b.expiry() - System.currentTimeMillis());
        String appeal = cachedAppeal;
        try {
            String live = plugin.getConfig().getString("punishments.appeal-url");
            if (live != null && !live.isEmpty()) appeal = live;
        } catch (Throwable ignored) {}

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i)
                    .replace("%player%", b.name())
                    .replace("%reason%", b.reason())
                    .replace("%check%", b.check())
                    .replace("%id%", b.id())
                    .replace("%expires%", expStr)
                    .replace("%appeal%", appeal);
            sb.append(Chat.color(l));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent e) {
        Ban b = bans.get(e.getUniqueId());
        if (b != null) {
            if (b.expired()) {
                bans.remove(e.getUniqueId());
                save();
                return;
            }
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, screen(b));
        }
    }

    private void broadcast(Ban b) {
        if (!cachedBroadcastEnabled) {
            try {
                if (!plugin.getConfig().getBoolean("punishments.broadcast.enabled", true)) return;
            } catch (Throwable ignored) { return; }
        }
        String fmt = cachedBroadcastFmt;
        try {
            String live = plugin.getConfig().getString("punishments.broadcast.format");
            if (live != null && !live.isEmpty()) fmt = live;
        } catch (Throwable ignored) {}
        final String finalFmt = fmt;
        Runnable task = () -> {
            try {
                Bukkit.broadcastMessage(Chat.color(finalFmt
                        .replace("%player%", b.name())
                        .replace("%reason%", b.reason())
                        .replace("%check%", b.check())
                        .replace("%id%", b.id())));
            } catch (Throwable ignored) {}
        };
        try {
            if (Platform.isPrimary()) task.run();
            else Platform.global(plugin, task);
        } catch (Throwable ignored) {
            try { Bukkit.getScheduler().runTask(plugin, task); } catch (Throwable ignored2) {}
        }
    }

    public static long parse(String s) {
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("perm")) return -1;
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : s.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) num.append(c);
            else if (num.length() > 0) {
                long n = Long.parseLong(num.toString());
                num.setLength(0);
                switch (c) {
                    case 'd': total += n * 86400000L; break;
                    case 'h': total += n * 3600000L; break;
                    case 'm': total += n * 60000L; break;
                    case 's': total += n * 1000L; break;
                }
            }
        }
        return total > 0 ? total : (num.length() > 0 ? Long.parseLong(num.toString()) * 86400000L : -1);
    }

    public static String fmt(long ms) {
        if (ms <= 0) return "Expired";
        long s = ms / 1000, d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (d == 0 && s % 60 > 0) sb.append(s % 60).append("s");
        return sb.toString().trim();
    }

    private void load() {
        if (!file.exists()) return;
        try (FileReader r = new FileReader(file)) {
            Type t = new TypeToken<Map<UUID, Ban>>() {}.getType();
            Map<UUID, Ban> d = gson.fromJson(r, t);
            if (d != null) bans.putAll(d);
        } catch (Exception ignored) {}
    }

    private synchronized void save() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) { gson.toJson(bans, w); }
        } catch (Exception ignored) {}
    }
}
