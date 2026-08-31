package ac.apex.packet;

import ac.apex.Apex;
import ac.apex.check.impl.combat.aim.*;
import ac.apex.check.impl.combat.autoclicker.*;
import ac.apex.check.impl.movement.ground.GroundSpoofA;
import ac.apex.check.impl.movement.motion.MotionA;
import ac.apex.check.impl.movement.timer.TimerA;
import ac.apex.check.impl.world.block.BreakA;
import ac.apex.check.impl.world.block.PlaceA;
import ac.apex.data.PlayerData;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Packets implements PacketListener {
    private final Apex plugin;

    public Packets(Apex plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent e) {
        Player p = (Player) e.getPlayer();
        if (p == null) return;

        PlayerData d = plugin.data().get(p);
        if (d == null) return;

        if (e.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition pos = new WrapperPlayClientPlayerPosition(e);
            pos(d, pos.getPosition().getX(), pos.getPosition().getY(), pos.getPosition().getZ(), pos.isOnGround());
        } else if (e.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation rot = new WrapperPlayClientPlayerRotation(e);
            rot(d, rot.getYaw(), rot.getPitch());
        } else if (e.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation pr = new WrapperPlayClientPlayerPositionAndRotation(e);
            pos(d, pr.getPosition().getX(), pr.getPosition().getY(), pr.getPosition().getZ(), pr.isOnGround());
            rot(d, pr.getYaw(), pr.getPitch());
        } else if (e.getPacketType() == PacketType.Play.Client.ANIMATION) {
            d.setLastSwing(System.currentTimeMillis());
            AutoClickerA a = d.get(AutoClickerA.class);
            if (a != null) a.click();
            AutoClickerB b = d.get(AutoClickerB.class);
            if (b != null) b.click();
        } else if (e.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity it = new WrapperPlayClientInteractEntity(e);
            if (it.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                d.setLastAttack(System.currentTimeMillis());
            }
        } else if (e.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            d.ping().ack(new WrapperPlayClientWindowConfirmation(e).getActionId());
        } else if (e.getPacketType() == PacketType.Play.Client.PONG) {
            d.ping().ack(new WrapperPlayClientPong(e).getId());
        } else if (e.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction.Action act = new WrapperPlayClientEntityAction(e).getAction();
            if (act == WrapperPlayClientEntityAction.Action.START_SPRINTING) d.setSprint(true);
            else if (act == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) d.setSprint(false);
            else if (act == WrapperPlayClientEntityAction.Action.START_SNEAKING) d.setSneak(true);
            else if (act == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) d.setSneak(false);
        } else if (e.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(e);
            DiggingAction action = dig.getAction();
            if (action == DiggingAction.START_DIGGING || action == DiggingAction.FINISHED_DIGGING) {
                Vector3i bp = dig.getBlockPosition();
                try {
                    Location loc = new Location(p.getWorld(), bp.getX(), bp.getY(), bp.getZ());
                    BreakA check = d.get(BreakA.class);
                    if (check != null) check.process(p, loc);
                } catch (Throwable ignored) {}
            }
        } else if (e.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement place = new WrapperPlayClientPlayerBlockPlacement(e);
            try {
                Vector3i bp = place.getBlockPosition();
                Location loc = new Location(p.getWorld(), bp.getX(), bp.getY(), bp.getZ());
                Location target = loc;
                try {
                    com.github.retrooper.packetevents.protocol.world.BlockFace face = place.getFace();
                    if (face != null) target = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                } catch (Throwable ignored) {}
                PlaceA check = d.get(PlaceA.class);
                if (check != null) check.process(p, target);
            } catch (Throwable ignored) {}
        }
    }

    private void pos(PlayerData d, double x, double y, double z, boolean g) {
        d.pos(x, y, z, g);
        TimerA t = d.get(TimerA.class);
        if (t != null) t.process();
        MotionA m = d.get(MotionA.class);
        if (m != null) m.process(d.dx(), d.dy(), d.dz(), g);
        GroundSpoofA gs = d.get(GroundSpoofA.class);
        if (gs != null) gs.process(d.dy(), g);
    }

    private void rot(PlayerData d, float yaw, float pitch) {
        d.rot(yaw, pitch);
        AimA a = d.get(AimA.class);
        if (a != null) a.process(d.dyaw(), d.dpitch());
        AimB b = d.get(AimB.class);
        if (b != null) b.process(d.dyaw(), d.dpitch());
        AimC c = d.get(AimC.class);
        if (c != null) c.process(yaw, d.dyaw(), d.dx(), d.dz());
    }
}
