package ac.apex.check.impl.combat.aim;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;

@CheckInfo(name = "Aim", type = "C", description = "Checks for silent aim rotation direction inversion", category = Category.COMBAT, config = "rotation-snap")
public class AimC extends Check {
    private float lastYaw = 0.0f;

    public AimC(PlayerData data) {
        super(data);
    }

    public void process(float yaw, float dyaw, double fwd, double str) {
        if (dyaw > 35.0f && (Math.abs(fwd) > 0 || Math.abs(str) > 0)) {
            if (System.currentTimeMillis() - data.lastAttack() < 250) {
                double moveAngle = Math.toDegrees(Math.atan2(str, fwd)) - 90;
                double cur = Maths.wrap((float) (yaw + moveAngle));
                double prev = Maths.wrap((float) (lastYaw + moveAngle));

                double diff = Math.abs(cur - prev);
                if (diff < 5.0 && dyaw > 30.0f) {
                    fail(String.format("dyaw=%.1f, angleDiff=%.1f (Silent Inversion)", dyaw, diff));
                }
            }
        }
        this.lastYaw = yaw;
    }
}
