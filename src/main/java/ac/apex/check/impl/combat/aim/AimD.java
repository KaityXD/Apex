package ac.apex.check.impl.combat.aim;

import ac.apex.Apex;
import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Aim", type = "D", description = "Detects perfect aim lock with configurable offset", category = Category.COMBAT, config = "aim-precision")
public class AimD extends Check {
    private double buf = 0.0;
    private long lastFlag = 0;

    public AimD(PlayerData data) {
        super(data);
    }

    public void process(Player attacker, Location victimLoc, double w, double h) {
        if (attacker.isFlying() || attacker.isInsideVehicle() || attacker.isDead() || data.teleporting()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        long ping = data.ping().ms();
        if (ping > 280) {
            buf = Math.max(0, buf - 0.4);
            return;
        }

        double thr = 0.05;
        try {
            Apex pl = Apex.get();
            if (pl != null) thr = pl.getConfig().getDouble("checks.combat.aim-precision.threshold", 0.05);
            if (thr < 0.01) thr = 0.01;
            if (thr > 0.15) thr = 0.15;
        } catch (Throwable ignored) {}

        Location eye = attacker.getEyeLocation();
        if (eye.getWorld() != victimLoc.getWorld()) {
            buf = Math.max(0, buf - 0.4);
            return;
        }

        double vx = victimLoc.getX();
        double vy = victimLoc.getY() + h * 0.5;
        double vz = victimLoc.getZ();
        Vector eyePos = eye.toVector();
        Vector target = new Vector(vx - eyePos.getX(), vy - eyePos.getY(), vz - eyePos.getZ());
        double dist = target.length();
        if (dist < 1.0 || dist > 6.5) {
            buf = Math.max(0, buf - 0.3);
            return;
        }
        target.normalize();
        Vector dir = eye.getDirection();
        dir.normalize();
        double dot = dir.dot(target);
        if (dot > 1.0) dot = 1.0;
        if (dot < -1.0) dot = -1.0;
        double angle = Math.toDegrees(Math.acos(dot));
        double offset = Math.sin(Math.toRadians(angle)) * dist;

        if (offset < thr && angle < 3.0) {
            long now = System.currentTimeMillis();
            boolean recent = (now - lastFlag) < 2500;
            double inc = 1.0;
            if (offset < thr * 0.5) inc = 1.4;
            if (!recent && buf < 1.0) inc *= 0.7;
            buf += inc;
            lastFlag = now;
            debug(String.format("perfect offset=%.4f angle=%.2f thr=%.3f buf=%.1f", offset, angle, thr, buf));
            if (buf >= 4.0) {
                fail(String.format("offset=%.4f thr=%.3f dist=%.2f ping=%d", offset, thr, dist, ping), 1.0);
                buf = 1.5;
            }
        } else {
            buf = Math.max(0, buf - 0.4);
        }
    }
}
