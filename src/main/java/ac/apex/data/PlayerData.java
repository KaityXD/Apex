package ac.apex.data;

import ac.apex.Apex;
import ac.apex.check.Check;
import ac.apex.check.impl.combat.aim.AimA;
import ac.apex.check.impl.combat.aim.AimB;
import ac.apex.check.impl.combat.aim.AimC;
import ac.apex.check.impl.combat.aim.AimD;
import ac.apex.check.impl.combat.autoclicker.AutoClickerA;
import ac.apex.check.impl.combat.autoclicker.AutoClickerB;
import ac.apex.check.impl.combat.aura.AuraA;
import ac.apex.check.impl.combat.reach.ReachA;
import ac.apex.check.impl.movement.ground.GroundSpoofA;
import ac.apex.check.impl.movement.motion.MotionA;
import ac.apex.check.impl.movement.timer.TimerA;
import ac.apex.check.impl.badpackets.BadPacketsA;
import ac.apex.check.impl.movement.fly.FlyA;
import ac.apex.check.impl.movement.inventory.InventoryA;
import ac.apex.check.impl.movement.jesus.JesusA;
import ac.apex.check.impl.movement.nofall.NoFallA;
import ac.apex.check.impl.movement.noslow.NoSlowA;
import ac.apex.check.impl.movement.strafe.StrafeA;
import ac.apex.check.impl.movement.velocity.VelocityA;
import ac.apex.check.impl.world.block.BreakA;
import ac.apex.check.impl.world.block.BreakB;
import ac.apex.check.impl.world.block.PlaceA;
import ac.apex.check.impl.world.block.PlaceB;
import ac.apex.compat.Platform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    private final Player p;
    private final Ping ping;
    private final World world;
    private final List<Check> checks = new ArrayList<>();

    private double x, y, z, lastX, lastY, lastZ, dx, dy, dz;
    private float yaw, pitch, lastYaw, lastPitch, dyaw, dpitch;
    private double vx, vz, lastVx, lastVz;
    private boolean ground, lastGround, sprint, sneak, teleporting;
    private Location setback;
    private long lastAttack, lastSwing;
    private long lastTeleportMs = 0;
    private long lastVelocityMs = 0;

    private volatile boolean cachedFlying = false;
    private volatile boolean cachedVehicle = false;
    private volatile boolean cachedDead = false;
    private volatile boolean cachedGliding = false;
    private volatile boolean cachedSwimming = false;
    private volatile boolean cachedClimbing = false;
    private volatile boolean cachedInLiquid = false;
    private volatile boolean cachedSpecialBlock = false;
    private volatile boolean cachedMovementPotion = false;
    private volatile double cachedMoveSpeed = 0.1;
    private volatile boolean inventoryOpen = false;
    private volatile boolean usingItem = false;
    private volatile long usingItemSince = 0;
    private volatile boolean cachedEntityPush = false;

    public PlayerData(Player player) {
        this.p = player;
        this.world = player.getWorld();
        this.ping = new Ping(this);
        this.setback = player.getLocation().clone();

        checks.add(new AimA(this));
        checks.add(new AimB(this));
        checks.add(new AimC(this));
        checks.add(new AimD(this));
        checks.add(new AutoClickerA(this));
        checks.add(new AutoClickerB(this));
        checks.add(new ReachA(this));
        checks.add(new AuraA(this));
        checks.add(new MotionA(this));
        checks.add(new GroundSpoofA(this));
        checks.add(new TimerA(this));
        checks.add(new BreakA(this));
        checks.add(new BreakB(this));
        checks.add(new PlaceA(this));
        checks.add(new PlaceB(this));
        checks.add(new StrafeA(this));
        checks.add(new VelocityA(this));
        checks.add(new NoSlowA(this));
        checks.add(new InventoryA(this));
        checks.add(new NoFallA(this));
        checks.add(new JesusA(this));
        checks.add(new FlyA(this));
        checks.add(new BadPacketsA(this));
    }

    public <T extends Check> T get(Class<T> clazz) {
        for (Check c : checks) {
            if (clazz.isInstance(c)) return clazz.cast(c);
        }
        return null;
    }

    public void setback() {
        if (setback == null) return;
        Apex plugin = Apex.get();
        if (plugin == null) return;
        Location loc = setback.clone();
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            try { p.teleport(loc); } catch (Exception ignored) {}
        } else {
            try {
                Platform.entity(plugin, p, e -> {
                    try { e.teleport(loc); } catch (Exception ignored) {}
                });
            } catch (Exception ex) {
                try { org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    try { p.teleport(loc); } catch (Exception ignored) {}
                }); } catch (Exception ignored) {}
            }
        }
    }

    public void markVelocity() {
        this.lastVelocityMs = System.currentTimeMillis();
    }

    public boolean hasRecentVelocity(long ms) {
        return System.currentTimeMillis() - lastVelocityMs < ms;
    }

    public void markTeleport(Location loc) {
        this.teleporting = true;
        this.lastTeleportMs = System.currentTimeMillis();
        if (loc != null) {
            this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
            this.lastX = this.x; this.lastY = this.y; this.lastZ = this.z;
            this.dx = 0; this.dy = 0; this.dz = 0;
            this.vx = 0; this.vz = 0; this.lastVx = 0; this.lastVz = 0;
            this.setback = loc.clone();
        }
        Apex plugin = Apex.get();
        if (plugin != null) {
            try {
                if (Platform.FOLIA) {
                    p.getScheduler().runDelayed(plugin, (t) -> this.teleporting = false, null, 6L);
                } else {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> this.teleporting = false, 6L);
                }
            } catch (Throwable ignored) {
                try { org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> this.teleporting = false, 6L); } catch (Throwable ignored2) {}
            }
        }
    }

    public void pos(double nx, double ny, double nz, boolean g) {
        this.lastX = this.x; this.lastY = this.y; this.lastZ = this.z;
        this.x = nx; this.y = ny; this.z = nz;
        this.dx = this.x - this.lastX; this.dy = this.y - this.lastY; this.dz = this.z - this.lastZ;
        this.lastGround = this.ground;
        this.ground = g;
        if (g && !teleporting) {
            try {
                this.setback = new Location(world, nx, ny, nz, this.yaw, this.pitch);
            } catch (Exception ignored) {}
        }
        if (teleporting && System.currentTimeMillis() - lastTeleportMs > 600) {
            teleporting = false;
        }
    }

    public void rot(float nyaw, float npitch) {
        this.lastYaw = this.yaw; this.lastPitch = this.pitch;
        this.yaw = nyaw; this.pitch = npitch;
        this.dyaw = Math.abs(this.yaw - this.lastYaw);
        this.dpitch = Math.abs(this.pitch - this.lastPitch);
    }

    public void vel(double nvx, double nvz) {
        this.lastVx = this.vx; this.lastVz = this.vz;
        this.vx = nvx; this.vz = nvz;
    }

    public Player player() { return p; }
    public String name() { return p.getName(); }
    public ClientVersion version() { return PacketEvents.getAPI().getPlayerManager().getClientVersion(p); }
    public Ping ping() { return ping; }
    public List<Check> checks() { return checks; }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double dx() { return dx; }
    public double dy() { return dy; }
    public double dz() { return dz; }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float dyaw() { return dyaw; }
    public float dpitch() { return dpitch; }

    public double vx() { return vx; }
    public double vz() { return vz; }
    public double lastVx() { return lastVx; }
    public double lastVz() { return lastVz; }

    public boolean ground() { return ground; }
    public boolean lastGround() { return lastGround; }
    public boolean sprint() { return sprint; }
    public boolean sneak() { return sneak; }
    public boolean teleporting() { return teleporting; }

    public long lastAttack() { return lastAttack; }
    public long lastSwing() { return lastSwing; }

    public void setSprint(boolean s) { this.sprint = s; }
    public void sSpr(boolean s) { setSprint(s); }
    public void setSneak(boolean s) { this.sneak = s; }
    public void sSnk(boolean s) { setSneak(s); }
    public void setTeleporting(boolean t) { this.teleporting = t; }
    public void setLastAttack(long t) { this.lastAttack = t; }
    public void setLastSwing(long t) { this.lastSwing = t; }
    public boolean isInventoryOpen() { return inventoryOpen; }
    public boolean isInv() { return inventoryOpen; }
    public void setInventoryOpen(boolean o) { this.inventoryOpen = o; }
    public void setInv(boolean o) { setInventoryOpen(o); }
    public boolean isUsingItem() { return usingItem; }
    public boolean isUse() { return usingItem; }
    public void setUsingItem(boolean u) { this.usingItem = u; this.usingItemSince = u ? System.currentTimeMillis() : 0; }
    public void setUse(boolean u) { setUsingItem(u); }
    public Player pl() { return p; }
    public String n() { return name(); }
    public Ping pg() { return ping; }
    public <T extends Check> T g(Class<T> c) { return get(c); }
    public void sb() { setback(); }
    public void tp(Location loc) { markTeleport(loc); }
    public void velM() { markVelocity(); }
    public boolean hasVel(long ms) { return hasRecentVelocity(ms); }
    public void tick() { tickUpdate(); }

    public void tickUpdate() {
        try { cachedFlying = p.isFlying(); } catch (Throwable ignored) {}
        try { cachedVehicle = p.isInsideVehicle(); } catch (Throwable ignored) {}
        try { cachedDead = p.isDead(); } catch (Throwable ignored) {}
        try { cachedGliding = p.isGliding(); } catch (Throwable ignored) { cachedGliding = false; }
        try { cachedSwimming = p.isSwimming(); } catch (Throwable ignored) { cachedSwimming = false; }
        try {
            Object o = p.getClass().getMethod("isClimbing").invoke(p);
            cachedClimbing = o instanceof Boolean && (Boolean) o;
        } catch (Throwable ignored) {
            try {
                String n = p.getLocation().getBlock().getType().name();
                cachedClimbing = n.contains("LADDER") || n.contains("VINE") || n.contains("SCAFFOLDING");
            } catch (Throwable ignored2) { cachedClimbing = false; }
        }
        try {
            cachedInLiquid = p.isInWater() || p.isInLava();
            if (!cachedInLiquid) {
                String n = p.getLocation().getBlock().getType().name();
                if (n.contains("WATER") || n.contains("LAVA") || n.contains("BUBBLE") || n.contains("COBWEB") || n.contains("POWDER")) cachedInLiquid = true;
            }
        } catch (Throwable ignored) {
            try {
                String n = p.getLocation().getBlock().getType().name();
                cachedInLiquid = n.contains("WATER") || n.contains("LAVA");
            } catch (Throwable ignored2) { cachedInLiquid = false; }
        }
        try {
            org.bukkit.block.Block under = p.getLocation().subtract(0, 0.5, 0).getBlock();
            org.bukkit.block.Block at = p.getLocation().getBlock();
            String n1 = under.getType().name();
            String n2 = at.getType().name();
            cachedSpecialBlock = n1.contains("ICE") || n1.contains("SLIME") || n1.contains("HONEY") || n1.contains("SOUL") || n1.contains("SCAFFOLDING") || n1.contains("POWDER")
                    || n2.contains("ICE") || n2.contains("SLIME") || n2.contains("HONEY") || n2.contains("SOUL");
        } catch (Throwable ignored) { cachedSpecialBlock = false; }
        try {
            boolean pot = false;
            if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) pot = true;
            else if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW)) pot = true;
            else if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.JUMP)) pot = true;
            else {
                try { if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION)) pot = true; } catch (Throwable ignored) {}
                try { if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING)) pot = true; } catch (Throwable ignored) {}
                try { if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE)) pot = true; } catch (Throwable ignored) {}
            }
            cachedMovementPotion = pot;
        } catch (Throwable ignored) { cachedMovementPotion = false; }
        try {
            Object attr = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr != null) {
                double v = ((org.bukkit.attribute.AttributeInstance) attr).getValue();
                if (v > 0 && v < 2) cachedMoveSpeed = v;
            }
        } catch (Throwable ignored) {}
        try {
            if (p.getVelocity().lengthSquared() > 0.004) lastVelocityMs = System.currentTimeMillis();
        } catch (Throwable ignored) {}
        try {
            boolean push = false;
            for (org.bukkit.entity.Entity e : p.getNearbyEntities(1.0, 1.0, 1.0)) {
                if (e == p) continue;
                if (e instanceof Player && ((Player) e).getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                push = true;
                break;
            }
            cachedEntityPush = push;
        } catch (Throwable ignored) {
            cachedEntityPush = false;
        }
        if (usingItem && usingItemSince != 0 && System.currentTimeMillis() - usingItemSince > 1500) {
            usingItem = false;
            usingItemSince = 0;
        }
    }

    public boolean cachedFlying() { return cachedFlying; }
    public boolean cachedVehicle() { return cachedVehicle; }
    public boolean cachedDead() { return cachedDead; }
    public boolean cachedGliding() { return cachedGliding; }
    public boolean cachedSwimming() { return cachedSwimming; }
    public boolean cachedClimbing() { return cachedClimbing; }
    public boolean cachedInLiquid() { return cachedInLiquid; }
    public boolean cachedSpecialBlock() { return cachedSpecialBlock; }
    public boolean cachedMovementPotion() { return cachedMovementPotion; }
    public double cachedMoveSpeed() { return cachedMoveSpeed; }
    public boolean cachedEntityPush() { return cachedEntityPush; }
}
