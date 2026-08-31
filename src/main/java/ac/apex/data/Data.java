package ac.apex.data;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Data {
    private final Map<UUID, PlayerData> map = new ConcurrentHashMap<>();

    public PlayerData get(Player p) { return map.computeIfAbsent(p.getUniqueId(), u -> new PlayerData(p)); }
    public PlayerData g(Player p) { return get(p); }
    public PlayerData get(UUID u) { return map.get(u); }
    public PlayerData g(UUID u) { return get(u); }
    public void remove(Player p) { map.remove(p.getUniqueId()); }
    public void r(Player p) { remove(p); }
    public Collection<PlayerData> all() { return map.values(); }
}
