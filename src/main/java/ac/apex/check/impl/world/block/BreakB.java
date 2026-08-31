package ac.apex.check.impl.world.block;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "Break", type = "B", description = "Detects breaking blocks too quickly", category = Category.WORLD, config = "break-fast")
public class BreakB extends Check {
    private long lastBreak = 0;
    private double buf = 0.0;
    private long lastTick = -1;
    private int brokenThisTick = 0;

    public BreakB(PlayerData data) {
        super(data);
    }

    public void process(Player player) {
        if (player.isFlying() || player.isInsideVehicle() || player.isDead() || data.teleporting()) {
            buf = Math.max(0, buf - 0.5);
            lastBreak = 0;
            brokenThisTick = 0;
            return;
        }

        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.4);
            brokenThisTick = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long tick = now / 50;

        if (tick == lastTick) {
            brokenThisTick++;
            if (brokenThisTick > 1) {
                buf += 1.2;
                debug(String.format("fastbreak tick=%d count=%d buf=%.1f ping=%d", tick, brokenThisTick, buf, ping));
                if (buf >= 3.0) {
                    fail(String.format("broke %d in tick %d ping=%d", brokenThisTick, tick, ping), 1.0);
                    buf = Math.max(1.0, buf - 2.0);
                }
            }
        } else {
            brokenThisTick = 1;
            buf = Math.max(0, buf - 0.35);
        }
        lastTick = tick;

        if (lastBreak != 0) {
            long delta = now - lastBreak;
            long threshold = 90;
            if (ping > 100) threshold += (ping - 100) * 2 / 5;
            if (threshold > 150) threshold = 150;
            if (delta < threshold && delta > 5) {
                double inc = 1.0 + (threshold - delta) * 0.015;
                if (inc > 1.8) inc = 1.8;
                buf += inc * 0.45;
                if (buf >= 3.0) {
                    fail(String.format("delta=%dms thr=%d ping=%d", delta, threshold, ping), 1.0);
                    buf = Math.max(1.0, buf - 2.0);
                }
            } else if (delta >= threshold) {
                buf = Math.max(0, buf - 0.2);
            }
        }
        lastBreak = now;
    }
}
