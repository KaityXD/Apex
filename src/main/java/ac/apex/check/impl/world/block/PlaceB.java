package ac.apex.check.impl.world.block;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "Place", type = "B", description = "Detects placing too many blocks per tick", category = Category.WORLD, config = "place-fast")
public class PlaceB extends Check {
    private long lastPlace = 0;
    private double buf = 0.0;
    private long lastTick = -1;
    private int placedThisTick = 0;

    public PlaceB(PlayerData data) {
        super(data);
    }

    public void process(Player player, boolean scaffolding) {
        if (player.isFlying() || player.isInsideVehicle() || player.isDead() || data.teleporting()) {
            buf = Math.max(0, buf - 0.5);
            lastPlace = 0;
            placedThisTick = 0;
            return;
        }

        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.4);
            placedThisTick = 0;
            return;
        }

        if (scaffolding) {
            buf = Math.max(0, buf - 0.6);
            placedThisTick = 0;
            lastPlace = System.currentTimeMillis();
            lastTick = lastPlace / 50;
            return;
        }

        long now = System.currentTimeMillis();
        long tick = now / 50;

        if (tick == lastTick) {
            placedThisTick++;
            if (placedThisTick > 1) {
                buf += 1.4;
                debug(String.format("fastplace tick=%d count=%d buf=%.1f ping=%d", tick, placedThisTick, buf, ping));
                if (buf >= 3.0) {
                    fail(String.format("placed %d in tick %d ping=%d", placedThisTick, tick, ping), 1.0);
                    buf = Math.max(1.0, buf - 2.0);
                }
            }
        } else {
            placedThisTick = 1;
            buf = Math.max(0, buf - 0.35);
        }
        lastTick = tick;

        if (lastPlace != 0) {
            long delta = now - lastPlace;
            long threshold = 90;
            if (ping > 100) threshold += (ping - 100) * 2 / 5;
            if (threshold > 150) threshold = 150;
            if (delta < threshold && delta > 5) {
                double inc = 1.0 + (threshold - delta) * 0.02;
                if (inc > 2.0) inc = 2.0;
                buf += inc * 0.5;
                if (buf >= 3.0) {
                    fail(String.format("delta=%dms thr=%d ping=%d", delta, threshold, ping), 1.0);
                    buf = Math.max(1.0, buf - 2.0);
                }
            } else if (delta >= threshold) {
                buf = Math.max(0, buf - 0.2);
            }
        }
        lastPlace = now;
    }
}
