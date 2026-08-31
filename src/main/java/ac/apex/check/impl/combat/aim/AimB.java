package ac.apex.check.impl.combat.aim;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;

@CheckInfo(name = "Aim", type = "B", description = "Checks for sudden multi-tick rotation acceleration", category = Category.COMBAT, config = "rotation-snap")
public class AimB extends Check {
    private double lastDelta = 0.0, prevDelta = 0.0;

    public AimB(PlayerData data) {
        super(data);
    }

    public void process(float dyaw, float dpitch) {
        boolean combat = (System.currentTimeMillis() - data.lastAttack()) < 300
                || (System.currentTimeMillis() - data.lastSwing()) < 300;

        if (prevDelta < 8.0 && lastDelta > 38.0 && dyaw < 8.0 && combat) {
            fail(String.format("snap=[%.1f/%.1f/%.1f]", prevDelta, lastDelta, dyaw));
        }

        prevDelta = lastDelta;
        lastDelta = dyaw;
    }
}
