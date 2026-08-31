package ac.apex.data;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Ping {
    private final PlayerData data;
    private final AtomicInteger seq = new AtomicInteger(1000);
    private final Map<Integer, Long> sent = new ConcurrentHashMap<>();
    private long ms = 0;

    public Ping(PlayerData data) {
        this.data = data;
    }

    public void send() {
        int id = seq.incrementAndGet();
        if (id > 32000) seq.set(1000);
        sent.put(id, System.currentTimeMillis());

        if (data.version().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(data.player(), new WrapperPlayServerPing(id));
        } else {
            PacketEvents.getAPI().getPlayerManager().sendPacket(data.player(), new WrapperPlayServerWindowConfirmation(0, (short) id, false));
        }
    }

    public void ack(int id) {
        Long t = sent.remove(id);
        if (t != null) this.ms = Math.max(0, System.currentTimeMillis() - t);
    }

    public long ms() { return ms; }
}
