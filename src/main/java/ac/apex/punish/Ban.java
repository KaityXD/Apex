package ac.apex.punish;

import java.util.UUID;

public class Ban {
    private final String id;
    private final UUID uuid;
    private final String name, reason, check;
    private final long time, expiry;

    public Ban(String id, UUID uuid, String name, String reason, String check, long time, long expiry) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.reason = reason;
        this.check = check;
        this.time = time;
        this.expiry = expiry;
    }

    public String id() { return id; }
    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public String reason() { return reason; }
    public String check() { return check; }
    public long time() { return time; }
    public long expiry() { return expiry; }
    public boolean perm() { return expiry <= 0; }
    public boolean expired() { return !perm() && System.currentTimeMillis() >= expiry; }
}
