package ac.apex.check.impl.movement.motion;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Motion", type = "A", description = "Lenient heuristic physics with multi-friction/speed hypotheses", category = Category.MOVEMENT, config = "simulation")
public class MotionA extends Check {
    private double buf = 0.0;

    private static final double BASE_EPSILON = 0.085;
    private static final double BUFFER_THRESHOLD = 7.5;
    private static final double MAX_INCREMENT = 2.0;

    public MotionA(PlayerData data) {
        super(data);
    }

    public void process(double dx, double dy, double dz, boolean ground) {
        Player p = data.player();
        double hDist = Maths.hypot(dx, dz);

        boolean flying = data.cachedFlying() || safeIsFlying(p);
        boolean vehicle = data.cachedVehicle() || safeIsVehicle(p);
        boolean dead = data.cachedDead();
        if (flying || vehicle || data.teleporting() || dead || data.hasRecentVelocity(700)) {
            if (data.hasRecentVelocity(700)) buf = Math.max(0, buf - 0.75);
            else buf = Math.max(0, buf - 1.0);
            data.vel(dx, dz);
            return;
        }

        if (data.cachedGliding() || data.cachedSwimming() || data.cachedClimbing() || isGliding(p) || isSwimming(p)) {
            buf = Math.max(0, buf - 1.0);
            data.vel(dx, dz);
            return;
        }

        if (data.cachedInLiquid() || isInLiquidOrClimbable(p)) {
            buf = Math.max(0, buf - 1.5);
            data.vel(dx, dz);
            return;
        }

        if (data.cachedEntityPush()) {
            buf = Math.max(0, buf - 0.75);
            data.vel(dx, dz);
            return;
        }

        if (hDist > 1.25 || Math.abs(dy) > 3.0) {
            buf = 0;
            data.vel(dx, dz);
            return;
        }

        try {
            if (p.getVelocity().lengthSquared() > 0.015) {
                data.markVelocity();
                buf = Math.max(0, buf - 0.75);
                data.vel(dx, dz);
                return;
            }
        } catch (Exception ignored) {}

        boolean hasPotion = data.cachedMovementPotion() || hasMovementPotion(p);
        boolean onSpecial = data.cachedSpecialBlock() || isOnSpecialBlock(p);

        long ping = data.ping().ms();
        if (ping > 350) {
            buf = Math.max(0, buf - 0.5);
            data.vel(dx, dz);
            return;
        }

        if (Math.abs(dy) > 0.52 && !ground) {
            buf = Math.max(0, buf - 0.3);
            data.vel(dx, dz);
            return;
        }

        if (hDist < 0.08) {
            buf = Math.max(0, buf - 0.35);
            data.vel(dx, dz);
            return;
        }

        double[] frictions = new double[]{
                0.6 * 0.91,
                0.91,
                0.98 * 0.91,
                0.8 * 0.91,
                0.42 * 0.91
        };

        double baseSpeed = getBaseSpeed(p);
        double[] speeds = new double[]{
                baseSpeed * 0.30,
                baseSpeed,
                baseSpeed * 1.30,
                baseSpeed * 1.30 * 1.20,
                baseSpeed * 1.30 * 1.40,
                0.02
        };

        double best = Double.MAX_VALUE;
        double[] inputs = {-1.0, 0.0, 1.0};

        for (double friction : frictions) {
            for (double speed : speeds) {
                for (double fwd : inputs) {
                    for (double str : inputs) {
                        double[] sim = sim(data.lastVx(), data.lastVz(), fwd, str, speed, friction);
                        double err = Maths.hypot(sim[0] - dx, sim[1] - dz);
                        if (err < best) best = err;
                    }
                }
            }
        }

        double epsilon = BASE_EPSILON;
        if (ping > 120) epsilon += (ping - 120) * 0.00055;
        if (!ground && !data.lastGround()) epsilon += 0.04;
        if (data.sneak()) epsilon += 0.02;
        if (hasPotion) epsilon += 0.07;
        if (onSpecial) epsilon += 0.06;

        if (epsilon > 0.32) epsilon = 0.32;

        if (best > epsilon && hDist > 0.20) {
            double inc = Math.min(MAX_INCREMENT, (best - epsilon) * 4.2);
            buf += inc;
            debug(String.format("motion offset=%.3f eps=%.3f inc=%.2f buf=%.1f hDist=%.3f ping=%d", best, epsilon, inc, buf, hDist, ping));
            if (buf > BUFFER_THRESHOLD) {
                fail(String.format("offset=%.3f, buf=%.1f, ping=%d", best, buf, ping), 1.0);
                setback();
                buf = Math.max(0, buf - 3.5);
            }
        } else if (best > epsilon && hDist <= 0.20 && hDist >= 0.08) {
            buf += Math.min(0.55, (best - epsilon) * 1.8);
            if (buf > BUFFER_THRESHOLD + 3) {
                fail(String.format("offset=%.3f, buf=%.1f (slow)", best, buf), 1.0);
                buf = Math.max(0, buf - 3.5);
            }
        } else {
            buf = Math.max(0.0, buf - 0.35);
        }

        if (hDist > 0.58 && ground && !data.sneak() && ping < 250) {
            double speedLimit = data.sprint() ? 0.45 : 0.33;
            if (data.sprint() && baseSpeed > 0.11) speedLimit += (baseSpeed - 0.1) * 2.5;
            if (hDist > speedLimit + 0.12) {
                buf += 0.9;
                if (buf > BUFFER_THRESHOLD) {
                    fail(String.format("speed hDist=%.3f limit=%.2f ping=%d", hDist, speedLimit, ping), 1.0);
                    setback();
                    buf = Math.max(0, buf - 3.5);
                }
            }
        }

        data.vel(dx, dz);
    }

    private double[] sim(double pvx, double pvz, double fwd, double str, double speed, double friction) {
        double dist = str * str + fwd * fwd;
        double mx = 0, mz = 0;

        if (dist >= 1.0E-4F) {
            dist = Math.sqrt(dist);
            if (dist < 1.0F) dist = 1.0F;
            dist = speed / dist;
            str *= dist;
            fwd *= dist;

            float yaw = data.yaw();
            double sin = Math.sin(Math.toRadians(yaw));
            double cos = Math.cos(Math.toRadians(yaw));

            mx = str * cos - fwd * sin;
            mz = fwd * cos + str * sin;
        }

        return new double[]{(pvx + mx) * friction, (pvz + mz) * friction};
    }

    private double getBaseSpeed(Player p) {
        double cached = data.cachedMoveSpeed();
        if (cached > 0.01 && cached < 2.0) return cached;
        try {
            Object attr = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr != null) {
                double v = ((org.bukkit.attribute.AttributeInstance) attr).getValue();
                if (v > 0 && v < 1.0) return v;
            }
        } catch (Throwable ignored) {}
        return data.sprint() ? 0.13000001 : 0.100000001;
    }

    private boolean safeIsFlying(Player p) {
        try { return p.isFlying(); } catch (Throwable e) { return false; }
    }
    private boolean safeIsVehicle(Player p) {
        try { return p.isInsideVehicle(); } catch (Throwable e) { return false; }
    }

    private boolean hasMovementPotion(Player p) {
        try {
            if (p.hasPotionEffect(PotionEffectType.SPEED)) return true;
            if (p.hasPotionEffect(PotionEffectType.SLOW)) return true;
            if (p.hasPotionEffect(PotionEffectType.JUMP)) return true;
            if (p.hasPotionEffect(PotionEffectType.LEVITATION)) return true;
            if (p.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return true;
            if (p.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isOnSpecialBlock(Player p) {
        try {
            Block under = p.getLocation().subtract(0, 0.5, 0).getBlock();
            Block atFeet = p.getLocation().getBlock();
            String[] names = new String[]{under.getType().name(), atFeet.getType().name()};
            for (String n : names) {
                if (n.contains("ICE") || n.contains("SLIME") || n.contains("HONEY")
                        || n.contains("SOUL") || n.contains("SCAFFOLDING") || n.contains("POWDER")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isInLiquidOrClimbable(Player p) {
        try {
            Block b = p.getLocation().getBlock();
            String n = b.getType().name();
            if (n.contains("WATER") || n.contains("LAVA") || n.contains("BUBBLE") || n.contains("CAULDRON")) return true;
            if (n.contains("LADDER") || n.contains("VINE") || n.contains("SCAFFOLDING") || n.contains("COBWEB") || n.contains("POWDER")) return true;
            if (p.isInWater() || p.isInLava()) return true;
        } catch (Throwable ignored) {}
        try {
            Material m = p.getLocation().getBlock().getType();
            if (m == Material.WATER || m == Material.LAVA) return true;
        } catch (Throwable ignored2) {}
        return false;
    }

    private boolean isGliding(Player p) {
        try { return p.isGliding(); } catch (Throwable e) { return false; }
    }

    private boolean isSwimming(Player p) {
        try { return p.isSwimming(); } catch (Throwable e) { return false; }
    }
}
