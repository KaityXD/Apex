package ac.apex.check.impl.combat.aura;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Aura", type = "A", description = "Detects attacks outside field of view", category = Category.COMBAT)
public class AuraA extends Check {
    private double buf = 0.0;
    private long lastFlag = 0;

    public AuraA(PlayerData data) {
        super(data);
    }

    public void process(Player attacker, Location victimLoc, double victimWidth, double victimHeight) {
        if (attacker.isFlying() || attacker.isInsideVehicle() || attacker.isDead() || data.teleporting()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }

        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.4);
            return;
        }

        Location eye = attacker.getEyeLocation();
        if (eye.getWorld() != victimLoc.getWorld()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }

        double w = victimWidth > 0 ? victimWidth : 0.6;
        double h = victimHeight > 0 ? victimHeight : 1.8;
        double vx = victimLoc.getX();
        double vy = victimLoc.getY() + h * 0.5;
        double vz = victimLoc.getZ();

        Vector eyePos = eye.toVector();
        Vector target = new Vector(vx - eyePos.getX(), vy - eyePos.getY(), vz - eyePos.getZ());
        double dist = target.length();
        if (dist > 6.5 || dist < 0.2) {
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

        double threshold = 50.0;
        if (ping > 80) threshold += (ping - 80) * 0.035;
        if (threshold > 72) threshold = 72;
        if (data.sprint()) threshold += 2.0;
        if (!data.ground()) threshold += 2.5;

        if (angle > threshold) {
            long now = System.currentTimeMillis();
            boolean recent = (now - lastFlag) < 2000;
            double inc = 1.0 + (angle - threshold) * 0.06;
            if (inc > 2.2) inc = 2.2;
            if (!recent && buf < 1.0) inc *= 0.7;
            buf += inc;
            lastFlag = now;
            debug(String.format("aura angle=%.1f thr=%.1f dist=%.2f buf=%.1f ping=%d", angle, threshold, dist, buf, ping));
            if (buf >= 3.0) {
                fail(String.format("angle=%.1f thr=%.1f dist=%.2f ping=%d", angle, threshold, dist, ping), 1.0);
                buf = Math.max(1.0, buf - 2.0);
            }
        } else {
            buf = Math.max(0, buf - 0.45);
        }
    }
}
