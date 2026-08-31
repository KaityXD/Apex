package ac.apex.check.impl.badpackets;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;

@CheckInfo(name = "BadPackets", type = "A", description = "Detects invalid packets", category = Category.MISC)
public class BadPacketsA extends Check {
    private double buf = 0.0;

    public BadPacketsA(PlayerData data) {
        super(data);
    }

    public void process(double x, double y, double z, float yaw, float pitch, boolean ground) {
        boolean invalid = false;
        String reason = "";

        if (Float.isNaN(yaw) || Float.isNaN(pitch) || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            invalid = true;
            reason = "nan rotation";
        } else if (Math.abs(pitch) > 90.5f) {
            invalid = true;
            reason = String.format("pitch=%.1f", pitch);
        } else if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            invalid = true;
            reason = "nan position";
        } else if (Math.abs(x) > 3.0E7 || Math.abs(z) > 3.0E7) {
            invalid = true;
            reason = "out of bounds";
        } else if (Math.abs(y) > 2048) {
            invalid = true;
            reason = "y out of bounds";
        }

        if (invalid) {
            buf += 2.0;
            debug(String.format("badpackets %s buf=%.1f", reason, buf));
            if (buf >= 2.0) {
                fail(reason, 1.0);
                buf = 0;
            }
        } else {
            buf = Math.max(0, buf - 0.5);
        }
    }
}
