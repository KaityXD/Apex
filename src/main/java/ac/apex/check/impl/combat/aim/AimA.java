package ac.apex.check.impl.combat.aim;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;

@CheckInfo(name = "Aim", type = "A", description = "Checks for mouse sensitivity GCD quantization", category = Category.COMBAT, config = "rotation-gcd")
public class AimA extends Check {
    private double prevGcd = 0.0, buf = 0.0;

    public AimA(PlayerData data) {
        super(data);
    }

    public void process(float dyaw, float dpitch) {
        if (dpitch <= 0.05f || dpitch > 30.0f) return;
        if (System.currentTimeMillis() - data.lastAttack() > 3000) return;

        double gcd = Maths.gcd(dpitch, prevGcd == 0 ? dpitch : prevGcd);
        double delta = Math.abs(gcd - prevGcd);
        this.prevGcd = gcd;

        if (delta > 0.001) {
            if (dpitch > 1.0f) buf += (dpitch > 5.0f ? 2.0 : 1.0);
            if (buf >= 15.0) {
                fail(String.format("dGCD=%.4f, dpitch=%.2f, buf=%.1f", delta, dpitch, buf));
                buf = 10.0;
            }
        } else {
            buf = Math.max(0.0, buf - 0.25);
        }
    }
}
