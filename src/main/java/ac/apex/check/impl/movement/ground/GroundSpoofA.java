package ac.apex.check.impl.movement.ground;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "GroundSpoof", type = "A", description = "Lenient on-ground spoof with block-aware exemptions", category = Category.MOVEMENT)
public class GroundSpoofA extends Check {
    private int buf = 0;

    public GroundSpoofA(PlayerData data) {
        super(data);
    }

    public void process(double dy, boolean ground) {
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead()) {
            buf = Math.max(0, buf - 1);
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) { buf = Math.max(0, buf - 1); return; }
        } catch (Throwable ignored) {}

        if (!ground || dy >= -0.12) {
            buf = Math.max(0, buf - 1);
            return;
        }

        if (data.ping().ms() > 300) { buf = Math.max(0, buf - 1); return; }
        try {
            if (p.getVelocity().lengthSquared() > 0.02) { buf = Math.max(0, buf - 1); return; }
        } catch (Throwable ignored) {}

        if (isNearGround(p)) {
            buf = Math.max(0, buf - 1);
            return;
        }

        if (isOnStairOrSlab(p)) {
            buf = Math.max(0, buf - 1);
            return;
        }

        double y = data.y();
        double frac = Math.abs(y % 0.015625);
        double distToGrid = Math.min(frac, 0.015625 - frac);

        if (distToGrid > 0.012) {
            if (++buf > 6) {
                fail(String.format("dy=%.3f, y=%.3f, offGrid=%.5f, buf=%d", dy, y, distToGrid, buf));
                buf = 3;
            }
        } else {
            buf = Math.max(0, buf - 1);
        }
    }

    private boolean isNearGround(Player p) {
        try {
            for (double o = 0.0; o <= 0.7; o += 0.35) {
                Block b = p.getLocation().subtract(0, o, 0).getBlock();
                if (b.getType().isSolid() && b.getType() != Material.AIR) {
                    return true;
                }
                String n = b.getType().name();
                if (n.contains("SLAB") || n.contains("STAIRS") || n.contains("CARPET") || n.contains("SNOW") || n.contains("FENCE")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isOnStairOrSlab(Player p) {
        try {
            Block under = p.getLocation().subtract(0, 0.5, 0).getBlock();
            Block at = p.getLocation().getBlock();
            String[] names = {under.getType().name(), at.getType().name()};
            for (String n : names) {
                if (n.contains("SLAB") || n.contains("STAIRS") || n.contains("CARPET") || n.contains("SNOW")
                        || n.contains("SCAFFOLDING") || n.contains("LADDER") || n.contains("VINE")
                        || n.contains("FENCE") || n.contains("WALL") || n.contains("TRAPDOOR") || n.contains("CAKE")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
