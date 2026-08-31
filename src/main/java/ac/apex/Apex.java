package ac.apex;

import ac.apex.command.Command;
import ac.apex.compat.Platform;
import ac.apex.data.Data;
import ac.apex.data.PlayerData;
import ac.apex.packet.Packets;
import ac.apex.punish.Punish;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Apex extends JavaPlugin implements Listener {
    private static Apex instance;
    private Data data;
    private Punish punish;
    private final Set<UUID> alertStaff = ConcurrentHashMap.newKeySet();
    private final Set<UUID> verboseStaff = ConcurrentHashMap.newKeySet();

    public static Apex get() {
        return instance;
    }

    public static Apex getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.data = new Data();
        this.punish = new Punish(this);

        PacketEvents.getAPI().getEventManager().registerListener(
                new Packets(this), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().init();

        Command cmd = new Command(this);
        if (getCommand("apex") != null) {
            getCommand("apex").setExecutor(cmd);
            getCommand("apex").setTabCompleter(cmd);
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        Platform.timer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData playerProfile = data.get(player);
                if (playerProfile != null) {
                    playerProfile.ping().send();
                }
            }
        }, 20L, 20L);

        Platform.timer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData pd = data.get(player);
                if (pd != null) {
                    try { pd.tickUpdate(); } catch (Throwable ignored) {}
                }
            }
        }, 1L, 2L);

        getLogger().info("==================================================");
        getLogger().info(" Apex Anti-Cheat v" + getDescription().getVersion());
        getLogger().info(" Multi-Version Architecture (1.8 - 26.2): Active");
        getLogger().info(" Engine Platform: " + (Platform.FOLIA ? "Folia" : "Paper/Spigot"));
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("Apex Anti-Cheat disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        data.get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        data.remove(event.getPlayer());
        alertStaff.remove(event.getPlayer().getUniqueId());
        verboseStaff.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerData pd = data.get(e.getPlayer());
        if (pd != null) {
            Location to = e.getTo();
            if (to != null) pd.markTeleport(to);
            else pd.markTeleport(e.getPlayer().getLocation());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        PlayerData pd = data.get(e.getPlayer());
        if (pd != null && e.getRespawnLocation() != null) pd.markTeleport(e.getRespawnLocation());
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent e) {
        Player p = e.getPlayer();
        if (p != null) {
            PlayerData pd = data.get(p);
            if (pd != null) {
                pd.markVelocity();
                try {
                    ac.apex.check.impl.movement.velocity.VelocityA v = pd.get(ac.apex.check.impl.movement.velocity.VelocityA.class);
                    if (v != null) v.onVelocity(e.getVelocity().getX(), e.getVelocity().getZ());
                } catch (Throwable ignored) {}
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                    || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                PlayerData pd = data.get(p);
                if (pd != null) pd.markVelocity();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player attacker = (Player) e.getDamager();
        PlayerData pd = data.get(attacker);
        if (pd == null) return;
        org.bukkit.Location victimLoc = e.getEntity().getLocation();
        double w = 0.6, h = 1.8;
        try {
            Object ent = e.getEntity();
            try {
                java.lang.reflect.Method mW = ent.getClass().getMethod("getWidth");
                java.lang.reflect.Method mH = ent.getClass().getMethod("getHeight");
                Object vw = mW.invoke(ent);
                Object vh = mH.invoke(ent);
                if (vw instanceof Number) w = ((Number) vw).doubleValue();
                if (vh instanceof Number) h = ((Number) vh).doubleValue();
            } catch (NoSuchMethodException nsme) {
            }
        } catch (Throwable ignored) {}
        try {
            String type = e.getEntityType().name();
            if (type.contains("ENDER_DRAGON")) { w = 8.0; h = 3.0; }
            else if (type.contains("GIANT")) { w = 3.6; h = 12.0; }
            else if (type.contains("WITHER")) { w = 0.9; h = 3.5; }
        } catch (Throwable ignored) {}
        try {
            ac.apex.check.impl.combat.reach.ReachA reach = pd.get(ac.apex.check.impl.combat.reach.ReachA.class);
            if (reach != null) reach.process(attacker, victimLoc, w, h);
        } catch (Throwable ignored) {}
        try {
            ac.apex.check.impl.combat.aura.AuraA aura = pd.get(ac.apex.check.impl.combat.aura.AuraA.class);
            if (aura != null) aura.process(attacker, victimLoc, w, h);
        } catch (Throwable ignored) {}
    }

    public boolean toggleAlerts(Player player) {
        if (alertStaff.contains(player.getUniqueId())) {
            alertStaff.remove(player.getUniqueId());
            return false;
        }
        alertStaff.add(player.getUniqueId());
        return true;
    }

    public boolean toggleVerbose(Player player) {
        if (verboseStaff.contains(player.getUniqueId())) {
            verboseStaff.remove(player.getUniqueId());
            return false;
        }
        verboseStaff.add(player.getUniqueId());
        return true;
    }

    public boolean alerts(Player player) {
        return alertStaff.contains(player.getUniqueId()) || player.hasPermission("apex.alerts");
    }

    public boolean verbose(Player player) {
        return verboseStaff.contains(player.getUniqueId());
    }

    public Data data() { return data; }
    public Punish punish() { return punish; }
}
