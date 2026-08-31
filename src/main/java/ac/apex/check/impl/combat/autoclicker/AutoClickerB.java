package ac.apex.check.impl.combat.autoclicker;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;

@CheckInfo(name = "AutoClicker", type = "B", description = "Checks for impossible double click frequencies", category = Category.COMBAT)
public class AutoClickerB extends Check {
    private long lastClick = 0;
    private int buf = 0;

    public AutoClickerB(PlayerData data) {
        super(data);
    }

    public void click() {
        long now = System.currentTimeMillis();
        if (lastClick != 0) {
            long delay = now - lastClick;
            if (delay < 15) {
                if (++buf > 2) {
                    fail(String.format("delay=%dms, buf=%d (Double Click)", delay, buf));
                    buf = 0;
                }
            } else {
                buf = Math.max(0, buf - 1);
            }
        }
        lastClick = now;
    }
}
