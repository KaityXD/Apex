package ac.apex.check.impl.combat.reach;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Reach", type = "A", description = "Checks for extended attack distance with lag compensation", category = Category.COMBAT, config = "reach")
public class ReachA extends Check {

    private double buf = 0.0;
    private long lastFlag = 0;

    private static final double BASE_MAX = 3.05;
    private static final double BUFFER_THRESHOLD = 4.5;

    public ReachA(PlayerData data) {
        super(data);
    }

    public void process(Player attacker, Location victimLoc, double victimWidth, double victimHeight) {
        if (attacker.isFlying() || attacker.isInsideVehicle() || attacker.isDead()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }

        long ping = data.ping().ms();
        if (ping > 320) {
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
        double vy = victimLoc.getY();
        double vz = victimLoc.getZ();

        double closestX = clamp(eye.getX(), vx - w/2 - 0.1, vx + w/2 + 0.1);
        double closestY = clamp(eye.getY(), vy, vy + h);
        double closestZ = clamp(eye.getZ(), vz - w/2 - 0.1, vz + w/2 + 0.1);

        Vector diff = new Vector(eye.getX() - closestX, eye.getY() - closestY, eye.getZ() - closestZ);
        double dist = diff.length();

        double slack = 0.12;
        if (ping > 80) slack += (ping - 80) * 0.0012;
        if (data.sprint()) slack += 0.04;
        if (!data.ground()) slack += 0.03;

        double max = BASE_MAX + slack;
        if (max > 3.85) max = 3.85;

        if (dist > max) {
            double excess = dist - max;
            double inc = Math.min(1.8, 0.6 + excess * 2.2);
            long now = System.currentTimeMillis();
            boolean recent = (now - lastFlag) < 2500;
            if (!recent && buf < 1.0) inc *= 0.5;
            buf += inc;
            lastFlag = now;
            debug(String.format("reach dist=%.3f max=%.3f excess=%.3f buf=%.1f ping=%d", dist, max, excess, buf, ping));
            if (buf >= BUFFER_THRESHOLD) {
                fail(String.format("dist=%.3f max=%.3f ping=%d", dist, max, ping), 1.0);
                buf = Math.max(1.0, buf - 3.0);
            }
        } else {
            buf = Math.max(0, buf - 0.45);
        }
    }

    public void process(Player attacker, Location victimLoc) {
        process(attacker, victimLoc, 0.6, 1.8);
    }

    private double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
