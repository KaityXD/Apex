package ac.apex.check.impl.movement.velocity;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.entity.Player;

@CheckInfo(name = "Velocity", type = "A", description = "Detects ignoring knockback", category = Category.MOVEMENT)
public class VelocityA extends Check {
    private double buf = 0.0;
    private long velocityTime = 0;
    private double expectedX = 0, expectedZ = 0;

    public VelocityA(PlayerData data) {
        super(data);
    }

    public void onVelocity(double vx, double vz) {
        velocityTime = System.currentTimeMillis();
        expectedX = vx;
        expectedZ = vz;
        buf = Math.max(0, buf - 0.5);
    }

    public void process(double dx, double dz, boolean ground) {
        if (velocityTime == 0) return;
        long now = System.currentTimeMillis();
        long delta = now - velocityTime;
        if (delta > 1200 || delta < 40) {
            if (delta > 1200) velocityTime = 0;
            return;
        }
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead()) {
            velocityTime = 0;
            return;
        }
        if (data.cachedGliding() || data.cachedSwimming() || data.cachedInLiquid()) {
            velocityTime = 0;
            return;
        }

        double exp = Maths.hypot(expectedX, expectedZ);
        if (exp < 0.15) {
            velocityTime = 0;
            return;
        }

        double hDist = Maths.hypot(dx, dz);
        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.3);
            return;
        }

        double ratio = hDist / Math.max(0.01, exp);
        if (delta < 600 && ratio < 0.15 && hDist < 0.08) {
            buf += 1.1;
            debug(String.format("velocity ratio=%.2f hDist=%.3f exp=%.3f buf=%.1f ping=%d", ratio, hDist, exp, buf, ping));
            if (buf >= 3.0) {
                fail(String.format("ratio=%.2f hDist=%.3f exp=%.3f ping=%d", ratio, hDist, exp, ping), 1.0);
                buf = 1.0;
                velocityTime = 0;
            }
        } else if (delta >= 600) {
            buf = Math.max(0, buf - 0.4);
            if (hDist > 0.1) velocityTime = 0;
        }
    }
}
