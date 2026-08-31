package ac.apex.check.impl.movement.strafe;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.entity.Player;

@CheckInfo(name = "Strafe", type = "A", description = "Detects invalid strafe movement", category = Category.MOVEMENT, config = "strafe")
public class StrafeA extends Check {
    private double buf = 0.0;

    public StrafeA(PlayerData data) {
        super(data);
    }

    public void process(double dx, double dz, float yaw) {
        Player p = data.player();
        double hDist = Maths.hypot(dx, dz);

        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead() || data.hasRecentVelocity(700)) {
            buf = Math.max(0, buf - 0.6);
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) { buf = Math.max(0, buf - 0.6); return; }
        } catch (Throwable ignored) {}
        if (data.cachedInLiquid() || data.cachedSpecialBlock() || data.cachedMovementPotion()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        if (data.cachedEntityPush()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300 || hDist < 0.15) {
            buf = Math.max(0, buf - 0.3);
            return;
        }

        double moveYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double diff = Math.abs(Maths.wrap((float) (yaw - moveYaw)));
        if (diff > 180) diff = 360 - diff;

        boolean sprint = data.sprint();
        double threshold = sprint ? 0.33 : 0.26;
        if (data.cachedMoveSpeed() > 0.12) threshold += (data.cachedMoveSpeed() - 0.1) * 1.2;

        boolean isStrafe = diff > 75 && diff < 115;
        boolean isDiagonal = diff > 35 && diff < 55 || diff > 125 && diff < 145;

        double limit = threshold;
        if (isStrafe) limit = sprint ? 0.29 : 0.22;
        if (isDiagonal) limit = sprint ? 0.38 : 0.30;

        if (hDist > limit + 0.07) {
            double excess = hDist - limit;
            double inc = Math.min(2.0, 0.7 + excess * 3.0);
            buf += inc;
            debug(String.format("strafe hDist=%.3f limit=%.2f diff=%.1f buf=%.1f ping=%d", hDist, limit, diff, buf, ping));
            if (buf >= 6.0) {
                fail(String.format("hDist=%.3f limit=%.2f diff=%.1f ping=%d", hDist, limit, diff, ping), 1.0);
                setback();
                buf = Math.max(2.0, buf - 3.0);
            }
        } else {
            buf = Math.max(0, buf - 0.35);
        }
    }
}
