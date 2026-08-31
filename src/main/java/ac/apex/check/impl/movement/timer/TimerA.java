package ac.apex.check.impl.movement.timer;

import ac.apex.check.Category;
import ac.apex.check.Check;
import ac.apex.check.CheckInfo;
import ac.apex.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "Timer", type = "A", description = "Lenient packet clock drift with lag compensation", category = Category.MOVEMENT)
public class TimerA extends Check {
    private long balance = 0;
    private long lastTime = 0;
    private double buf = 0;
    private long lastFlag = 0;

    public TimerA(PlayerData data) {
        super(data);
    }

    public void process() {
        Player p = data.player();
        if (p.isFlying() || p.isInsideVehicle() || data.teleporting() || p.isDead()) {
            balance = 0;
            buf = Math.max(0, buf - 0.5);
            lastTime = System.currentTimeMillis();
            return;
        }
        try {
            if (p.isGliding() || p.isSwimming()) {
                balance = Math.min(balance, 20);
                buf = Math.max(0, buf - 0.5);
                lastTime = System.currentTimeMillis();
                return;
            }
        } catch (Throwable ignored) {}

        long now = System.currentTimeMillis();
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        long delta = now - lastTime;
        lastTime = now;

        if (delta > 120) {
            if (delta > 250) balance = Math.max(-400, balance - 30);
            else balance = Math.max(-400, balance - 10);
            buf = Math.max(0, buf - 0.4);
            return;
        }
        if (delta < 2) return;

        long ping = data.ping().ms();
        if (ping > 300) {
            balance = Math.max(-400, balance - 5);
            buf = Math.max(0, buf - 0.3);
            return;
        }

        balance += (50 - delta);

        if (balance > 90) {
            long sinceLast = now - lastFlag;
            if (sinceLast < 4000) buf += 1.2;
            else buf = 1.0;
            lastFlag = now;

            debug(String.format("timer balance=%d delta=%d buf=%.1f ping=%d", balance, delta, buf, ping));

            if (buf >= 4.5) {
                fail(String.format("balance=%dms, buf=%.1f (Timer %.2fx)", balance, buf, 50.0 / Math.max(1, delta)));
                balance = 0;
                buf = 2.0;
            } else {
                balance = 25;
            }
        } else if (balance < -300) {
            balance = -300;
            buf = Math.max(0, buf - 0.35);
        } else {
            buf = Math.max(0, buf - 0.08);
            if (balance > 0) balance = Math.max(0, balance - 1);
            else if (balance < 0) balance = Math.min(0, balance + 1);
        }
    }
}
