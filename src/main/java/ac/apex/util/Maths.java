package ac.apex.util;

import java.util.Collection;

public final class Maths {

    public static double gcd(double a, double b) {
        if (a < b) { double t = a; a = b; b = t; }
        int count = 50;
        while (b > 0.0001 && count-- > 0) {
            double r = a % b;
            a = b;
            b = r;
        }
        return a;
    }

    public static double mean(Collection<Double> v) {
        if (v.isEmpty()) return 0.0;
        double s = 0;
        for (double d : v) s += d;
        return s / v.size();
    }

    public static double variance(Collection<Double> v) {
        if (v.size() < 2) return 0.0;
        double m = mean(v), t = 0;
        for (double d : v) t += (d - m) * (d - m);
        return t / v.size();
    }

    public static double stdDev(Collection<Double> v) {
        return Math.sqrt(variance(v));
    }

    public static double kurtosis(Collection<Double> v) {
        if (v.size() < 4) return 0.0;
        double m = mean(v), var = variance(v);
        if (var <= 0.0) return 0.0;
        double s4 = 0;
        for (double d : v) s4 += Math.pow(d - m, 4);
        return (s4 / (v.size() * var * var)) - 3.0;
    }

    public static double hypot(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static float wrap(float deg) {
        deg %= 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    private Maths() {}
}
