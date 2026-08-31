package ac.apex.check.impl.movement.fly;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "Fly", type = "A", description = "Detects flying without permission", category = Category.MOVEMENT, config = "fly")
public class FlyA extends Check {
    private double buf = 0.0;
    private int airTicks = 0;
    private double lastDy = 0;

    public FlyA(PlayerData data) {
        super(data);
    }

    public void process(double dy, boolean ground) {
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead() || data.hasRecentVelocity(700)) {
            buf = Math.max(0, buf - 0.6);
            airTicks = 0;
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) { buf = Math.max(0, buf - 0.6); airTicks = 0; return; }
        } catch (Throwable ignored) {}
        if (data.cachedInLiquid() || data.cachedClimbing() || data.cachedSpecialBlock() || data.cachedMovementPotion()) {
            buf = Math.max(0, buf - 0.4);
            airTicks = 0;
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.3);
            airTicks = 0;
            return;
        }

        if (!ground) {
            airTicks++;
            if (airTicks > 6) {
                if (dy > 0.1 && lastDy > 0.0 && dy > -0.08) {
                    buf += 1.0;
                    debug(String.format("fly dy=%.3f air=%d buf=%.1f", dy, airTicks, buf));
                    if (buf >= 6.0) {
                        fail(String.format("dy=%.3f air=%d ping=%d", dy, airTicks, ping), 1.0);
                        setback();
                        buf = 3.0;
                    }
                } else if (dy > -0.06 && airTicks > 20) {
                    buf += 0.7;
                    if (buf >= 8.0) {
                        fail(String.format("hover dy=%.3f air=%d ping=%d", dy, airTicks, ping), 1.0);
                        setback();
                        buf = 3.0;
                    }
                } else {
                    buf = Math.max(0, buf - 0.2);
                }
            }
        } else {
            airTicks = 0;
            buf = Math.max(0, buf - 0.5);
        }
        lastDy = dy;
    }
}
