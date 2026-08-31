package ac.apex.check.impl.movement.jesus;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "Jesus", type = "A", description = "Detects walking on water", category = Category.MOVEMENT)
public class JesusA extends Check {
    private double buf = 0.0;
    private int waterTicks = 0;

    public JesusA(PlayerData data) {
        super(data);
    }

    public void process(double dx, double dz, boolean ground) {
        Player p = data.player();
        double hDist = Maths.hypot(dx, dz);

        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead() || data.hasRecentVelocity(700)) {
            buf = Math.max(0, buf - 0.6);
            waterTicks = 0;
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) { buf = Math.max(0, buf - 0.6); waterTicks = 0; return; }
        } catch (Throwable ignored) {}
        if (data.cachedInLiquid()) {
            waterTicks = 0;
            buf = Math.max(0, buf - 0.4);
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.3);
            waterTicks = 0;
            return;
        }
        if (hDist < 0.12) {
            buf = Math.max(0, buf - 0.25);
            if (waterTicks > 0) waterTicks--;
            return;
        }

        boolean overWater = isOverWater(p);
        if (overWater && !ground) {
            waterTicks++;
            if (waterTicks > 3 && hDist > 0.18) {
                buf += 1.0;
                debug(String.format("jesus hDist=%.3f ticks=%d buf=%.1f", hDist, waterTicks, buf));
                if (buf >= 5.0) {
                    fail(String.format("hDist=%.3f ticks=%d ping=%d", hDist, waterTicks, ping), 1.0);
                    data.setback();
                    buf = 2.0;
                }
            }
        } else {
            waterTicks = Math.max(0, waterTicks - 1);
            buf = Math.max(0, buf - 0.35);
        }
    }

    private boolean isOverWater(Player p) {
        try {
            Block under = p.getLocation().subtract(0, 0.2, 0).getBlock();
            Material m = under.getType();
            String n = m.name();
            if (n.contains("WATER") || n.contains("KELP") || n.contains("SEAGRASS")) return true;
            Block at = p.getLocation().subtract(0, 1.0, 0).getBlock();
            String n2 = at.getType().name();
            if (n2.contains("WATER")) return true;
            if (under.isLiquid()) return true;
        } catch (Throwable ignored) {}
        return false;
    }
}
