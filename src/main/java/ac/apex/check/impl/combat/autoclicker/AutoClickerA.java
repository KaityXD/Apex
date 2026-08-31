package ac.apex.check.impl.combat.autoclicker;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import ac.apex.util.Maths;

import java.util.ArrayList;
import java.util.List;

@CheckInfo(name = "AutoClicker", type = "A", description = "Checks for abnormal click delay distribution kurtosis", category = Category.COMBAT)
public class AutoClickerA extends Check {
    private final List<Double> samples = new ArrayList<>();
    private long lastClick = 0;

    public AutoClickerA(PlayerData data) {
        super(data);
    }

    public void click() {
        long now = System.currentTimeMillis();
        if (lastClick != 0) {
            double delay = now - lastClick;
            if (delay < 400.0) {
                samples.add(delay);
                if (samples.size() >= 45) {
                    analyze();
                    samples.clear();
                }
            }
        }
        lastClick = now;
    }

    private void analyze() {
        double std = Maths.stdDev(samples);
        double kurt = Maths.kurtosis(samples);
        double mean = Maths.mean(samples);

        if (std < 4.5 && mean < 110.0) {
            fail(String.format("std=%.2fms, avg=%.1fms (Consistency)", std, mean));
            return;
        }

        if (Math.abs(kurt) > 12.0 && std < 9.0) {
            fail(String.format("kurt=%.2f, std=%.2fms", kurt, std));
        }
    }
}
