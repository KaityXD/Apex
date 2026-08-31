package ac.apex.check.impl.world.block;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Place", type = "A", description = "Detects placing blocks outside field of view", category = Category.WORLD)
public class PlaceA extends Check {
    private double buf = 0.0;
    private long lastFlag = 0;

    public PlaceA(PlayerData data) {
        super(data);
    }

    public void process(Player player, Location block) {
        if (player.isFlying() || player.isInsideVehicle() || player.isDead() || data.teleporting()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        try {
            if (player.isGliding() || player.isSwimming()) {
                buf = Math.max(0, buf - 0.5);
                return;
            }
        } catch (Throwable ignored) {}

        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.4);
            return;
        }

        Location eye = player.getEyeLocation();
        if (eye.getWorld() != block.getWorld()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }

        Vector eyePos = eye.toVector();
        Vector target = new Vector(block.getX() + 0.5 - eyePos.getX(), block.getY() + 0.5 - eyePos.getY(), block.getZ() + 0.5 - eyePos.getZ());
        double dist = target.length();
        if (dist > 6.5 || dist < 0.25) {
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

        double threshold = 55.0;
        if (ping > 80) threshold += (ping - 80) * 0.035;
        if (threshold > 75) threshold = 75;
        if (player.isSneaking() || data.sneak()) threshold += 2.0;
        if (!data.ground()) threshold += 3.0;

        if (angle > threshold) {
            long now = System.currentTimeMillis();
            boolean recent = (now - lastFlag) < 2000;
            double inc = 1.0 + (angle - threshold) * 0.07;
            if (inc > 2.5) inc = 2.5;
            if (!recent && buf < 1.0) inc *= 0.7;
            buf += inc;
            lastFlag = now;
            debug(String.format("place angle=%.1f thr=%.1f dist=%.2f buf=%.1f ping=%d", angle, threshold, dist, buf, ping));
            if (buf >= 3.0) {
                fail(String.format("angle=%.1f thr=%.1f dist=%.2f ping=%d", angle, threshold, dist, ping), 1.0);
                buf = Math.max(1.0, buf - 2.0);
            }
        } else {
            buf = Math.max(0, buf - 0.4);
        }
    }
}
