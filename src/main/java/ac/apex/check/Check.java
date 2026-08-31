package ac.apex.check;

import ac.apex.Apex;
import ac.apex.compat.Platform;
import ac.apex.data.PlayerData;
import ac.apex.util.Chat;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class Check {
    protected final PlayerData data;
    private final String name, type, desc;
    private final Category cat;
    private double vl = 0.0;
    private double maxVl = 20.0;
    private boolean enabled = true;
    private boolean autoBan = true;
    private String duration = "30d";

    public Check(PlayerData data) {
        this.data = data;
        CheckInfo info = getClass().getAnnotation(CheckInfo.class);
        if (info != null) {
            this.name = info.name();
            this.type = info.type();
            this.cat = info.category();
            this.desc = info.description();
        } else {
            this.name = getClass().getSimpleName();
            this.type = "A";
            this.cat = Category.MISC;
            this.desc = "";
        }
    }

    public String tag() {
        return name + " (" + type + ")";
    }

    public void fail(String info, double weight) {
        if (!enabled) return;
        this.vl += weight;

        Apex plugin = Apex.get();
        if (plugin == null) return;
        final double curVl = this.vl;
        final String playerName = data.name();
        final String tag = tag();
        final long ping = data.ping().ms();

        TextComponent msg = new TextComponent(Chat.color(String.format(
                "&8[&b&lAPEX&8] &b%s &7failed &f%s &8(&7VL: &b%.1f&8) ", playerName, tag, curVl)));

        TextComponent tp = new TextComponent(Chat.color("&8[&a&lTP&8] "));
        tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + playerName));
        tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Chat.color(String.format(
                "&bPlayer: &f%s\n&bCheck: &f%s\n&bVL: &f%.1f\n&bPing: &f%d ms\n&bInfo: &7%s\n\n&e▶ Click to teleport",
                playerName, tag, curVl, ping, info
        ))).create()));

        TextComponent ban = new TextComponent(Chat.color("&8[&c&lBAN&8]"));
        ban.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                String.format("/apex ban %s %s %s", playerName, duration, tag)));
        ban.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Chat.color("&c▶ Click to ban")).create()));

        msg.addExtra(tp);
        msg.addExtra(ban);

        Runnable alertTask = () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                try {
                    if (plugin.alerts(staff)) staff.spigot().sendMessage(msg);
                } catch (Throwable ignored) {}
            }
        };

        if (Platform.isPrimary()) {
            alertTask.run();
        } else {
            try { Platform.global(plugin, alertTask); } catch (Throwable ignored) {
                try { Bukkit.getScheduler().runTask(plugin, alertTask); } catch (Throwable ignored2) {}
            }
        }

        if (autoBan && curVl >= maxVl) {
            try { plugin.punish().execute(data.player(), "Unfair Advantage", tag, duration); } catch (Throwable ignored) {}
            vl = 0.0;
        }
    }

    public void fail(String info) {
        fail(info, 1.0);
    }

    public void debug(String info) {
        Apex plugin = Apex.get();
        if (plugin == null) return;
        String msg = Chat.color(String.format("&8[&7V&8] &b%s &8| &f%s &8| &7%s", data.name(), tag(), info));
        Runnable t = () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                try { if (plugin.verbose(staff)) staff.sendMessage(msg); } catch (Throwable ignored) {}
            }
        };
        if (Platform.isPrimary()) t.run();
        else try { Platform.global(plugin, t); } catch (Throwable ignored) { try { Bukkit.getScheduler().runTask(plugin, t); } catch (Throwable ignored2) {} }
    }

    public void decay(double n) { this.vl = Math.max(0.0, this.vl - n); }
    public String name() { return name; }
    public String type() { return type; }
    public Category cat() { return cat; }
    public String desc() { return desc; }
    public double vl() { return vl; }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }
    public void setMaxVl(double max) { this.maxVl = max; }
    public void setAutoBan(boolean b) { this.autoBan = b; }
    public void setDuration(String d) { this.duration = d; }
}
