package ac.apex.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class Platform {
    public static final boolean FOLIA;

    static {
        boolean f = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            f = true;
        } catch (ClassNotFoundException ignored) {}
        FOLIA = f;
    }

    public static void async(Plugin p, Runnable task) {
        if (FOLIA) Bukkit.getAsyncScheduler().runNow(p, t -> task.run());
        else Bukkit.getScheduler().runTaskAsynchronously(p, task);
    }

    public static void timer(Plugin p, Runnable task, long delay, long period) {
        if (FOLIA) Bukkit.getGlobalRegionScheduler().runAtFixedRate(p, t -> task.run(), Math.max(1, delay), Math.max(1, period));
        else Bukkit.getScheduler().runTaskTimer(p, task, delay, period);
    }

    public static void entity(Plugin p, Entity e, Consumer<Entity> action) {
        if (FOLIA) e.getScheduler().run(p, t -> action.accept(e), null);
        else Bukkit.getScheduler().runTask(p, () -> action.accept(e));
    }

    public static void global(Plugin p, Runnable task) {
        if (FOLIA) Bukkit.getGlobalRegionScheduler().run(p, t -> task.run());
        else Bukkit.getScheduler().runTask(p, task);
    }

    public static boolean isPrimary() {
        try { return Bukkit.isPrimaryThread(); } catch (Throwable t) { return false; }
    }

    private Platform() {}
}
