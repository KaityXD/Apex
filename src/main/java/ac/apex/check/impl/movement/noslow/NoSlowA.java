package ac.apex.check.impl.movement.noslow;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.entity.Player;

@CheckInfo(name = "NoSlow", type = "A", description = "Detects moving too fast while sneaking or using item", category = Category.MOVEMENT, config = "noslow")
public class NoSlowA extends Check {
    private double buf = 0.0;

    public NoSlowA(PlayerData data) {
        super(data);
    }

    public void process(double dx, double dz) {
        Player p = data.player();
        double hDist = Maths.hypot(dx, dz);

        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead() || data.hasRecentVelocity(700)) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        if (data.cachedInLiquid() || data.cachedSpecialBlock() || data.cachedMovementPotion()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        if (data.cachedEntityPush()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.4);
            return;
        }

        boolean sneaking = data.sneak() || data.cachedClimbing();
        boolean blocking = data.isUsingItem();

        if (!sneaking && !blocking) {
            buf = Math.max(0, buf - 0.3);
            return;
        }

        double walk = data.cachedMoveSpeed() * 2.2;
        double limit = sneaking ? 0.15 : walk + 0.04;
        if (sneaking && blocking) limit = 0.15;

        if (hDist > limit && hDist > 0.12) {
            double excess = hDist - limit;
            buf += Math.min(1.8, 0.6 + excess * 4.0);
            debug(String.format("noslow hDist=%.3f limit=%.2f sneak=%b block=%b buf=%.1f", hDist, limit, sneaking, blocking, buf));
            if (buf >= 5.0) {
                fail(String.format("hDist=%.3f limit=%.2f sneak=%b block=%b ping=%d", hDist, limit, sneaking, blocking, ping), 1.0);
                setback();
                buf = 2.0;
            }
        } else {
            buf = Math.max(0, buf - 0.35);
        }
    }
}
