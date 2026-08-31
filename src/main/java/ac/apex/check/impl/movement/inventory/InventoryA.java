package ac.apex.check.impl.movement.inventory;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;
import org.bukkit.entity.Player;

@CheckInfo(name = "Inventory", type = "A", description = "Detects moving with inventory open", category = Category.MOVEMENT, config = "inventory")
public class InventoryA extends Check {
    private double buf = 0.0;

    public InventoryA(PlayerData data) {
        super(data);
    }

    public void process(double dx, double dz) {
        if (!data.isInventoryOpen()) {
            buf = Math.max(0, buf - 0.4);
            return;
        }
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead()) {
            buf = Math.max(0, buf - 0.5);
            return;
        }
        double hDist = Maths.hypot(dx, dz);
        if (hDist < 0.12) {
            buf = Math.max(0, buf - 0.3);
            return;
        }
        long ping = data.ping().ms();
        if (ping > 300) {
            buf = Math.max(0, buf - 0.3);
            return;
        }

        buf += 1.0;
        debug(String.format("inventory hDist=%.3f buf=%.1f ping=%d", hDist, buf, ping));
        if (buf >= 4.0) {
            fail(String.format("hDist=%.3f ping=%d", hDist, ping), 1.0);
            setback();
            buf = 2.0;
        }
    }
}
