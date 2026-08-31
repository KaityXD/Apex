package ac.apex.check.impl.movement.nofall;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "NoFall", type = "A", description = "Detects avoiding fall damage", category = Category.MOVEMENT)
public class NoFallA extends Check {
    private double fallDistance = 0.0;
    private double buf = 0.0;
    private int airTicks = 0;

    public NoFallA(PlayerData data) {
        super(data);
    }

    public void process(double dy, boolean ground) {
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead() || data.hasRecentVelocity(800)) {
            fallDistance = 0;
            airTicks = 0;
            buf = Math.max(0, buf - 0.5);
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) { fallDistance = 0; airTicks = 0; return; }
        } catch (Throwable ignored) {}
        if (data.cachedInLiquid() || data.cachedClimbing() || data.cachedSpecialBlock()) {
            fallDistance = 0;
            airTicks = 0;
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300) {
            fallDistance = Math.max(0, fallDistance - 1.0);
            airTicks = 0;
            return;
        }

        if (!ground) {
            airTicks++;
            if (dy < -0.05) fallDistance += -dy;
            else if (dy > 0) fallDistance = Math.max(0, fallDistance - 0.5);
        } else {
            if (fallDistance > 3.2 && airTicks > 8) {
                double expectedDamage = fallDistance - 3.0;
                if (expectedDamage > 1.5) {
                    buf += 1.2;
                    debug(String.format("nofall fall=%.2f air=%d buf=%.1f", fallDistance, airTicks, buf));
                    if (buf >= 2.5) {
                        fail(String.format("fall=%.2f air=%d ping=%d", fallDistance, airTicks, ping), 1.0);
                        buf = 1.0;
                    }
                }
            } else {
                buf = Math.max(0, buf - 0.3);
            }
            fallDistance = 0;
            airTicks = 0;
        }
    }
}
