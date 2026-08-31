package ac.apex.packet;

import ac.apex.Apex;
import ac.apex.check.impl.badpackets.BadPacketsA;
import ac.apex.check.impl.combat.aim.*;
import ac.apex.check.impl.combat.autoclicker.*;
import ac.apex.check.impl.movement.fly.FlyA;
import ac.apex.check.impl.movement.ground.GroundSpoofA;
import ac.apex.check.impl.movement.inventory.InventoryA;
import ac.apex.check.impl.movement.jesus.JesusA;
import ac.apex.check.impl.movement.motion.MotionA;
import ac.apex.check.impl.movement.nofall.NoFallA;
import ac.apex.check.impl.movement.noslow.NoSlowA;
import ac.apex.check.impl.movement.strafe.StrafeA;
import ac.apex.check.impl.movement.timer.TimerA;
import ac.apex.check.impl.movement.velocity.VelocityA;
import ac.apex.check.impl.world.block.BreakA;
import ac.apex.check.impl.world.block.BreakB;
import ac.apex.check.impl.world.block.PlaceA;
import ac.apex.check.impl.world.block.PlaceB;
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
            if (action == DiggingAction.RELEASE_USE_ITEM) d.setUsingItem(false);
            if (action == DiggingAction.START_DIGGING || action == DiggingAction.FINISHED_DIGGING) {
                Vector3i bp = dig.getBlockPosition();
                try {
                    Location loc = new Location(p.getWorld(), bp.getX(), bp.getY(), bp.getZ());
                    BreakA check = d.get(BreakA.class);
                    if (check != null) check.process(p, loc);
                    BreakB fast = d.get(BreakB.class);
                    if (fast != null) fast.process(p);
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
                boolean scaffold = false;
                try {
                    java.util.Optional<com.github.retrooper.packetevents.protocol.item.ItemStack> opt = place.getItemStack();
                    if (opt.isPresent()) {
                        com.github.retrooper.packetevents.protocol.item.ItemStack stack = opt.get();
                        if (!stack.isEmpty()) {
                            String n = stack.getType().getName().toString();
                            if (n != null && n.toLowerCase().contains("scaffolding")) scaffold = true;
                            else if (stack.getType() == com.github.retrooper.packetevents.protocol.item.type.ItemTypes.SCAFFOLDING) scaffold = true;
                        }
                    }
                } catch (Throwable ignored) {}
                PlaceB fast = d.get(PlaceB.class);
                if (fast != null) fast.process(p, scaffold);
                d.setUsingItem(false);
            } catch (Throwable ignored) {}
        } else if (e.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            d.setUsingItem(true);
        } else if (e.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            d.setInventoryOpen(true);
        } else if (e.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            d.setInventoryOpen(false);
        } else if (e.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            d.setInventoryOpen(false);
        }
    }

    private void pos(PlayerData d, double x, double y, double z, boolean g) {
        d.pos(x, y, z, g);
        BadPacketsA bp = d.get(BadPacketsA.class);
        if (bp != null) bp.process(x, y, z, d.yaw(), d.pitch(), g);
        TimerA t = d.get(TimerA.class);
        if (t != null) t.process();
        MotionA m = d.get(MotionA.class);
        if (m != null) m.process(d.dx(), d.dy(), d.dz(), g);
        GroundSpoofA gs = d.get(GroundSpoofA.class);
        if (gs != null) gs.process(d.dy(), g);
        StrafeA strafe = d.get(StrafeA.class);
        if (strafe != null) strafe.process(d.dx(), d.dz(), d.yaw());
        VelocityA vel = d.get(VelocityA.class);
        if (vel != null) vel.process(d.dx(), d.dz(), g);
        NoSlowA ns = d.get(NoSlowA.class);
        if (ns != null) ns.process(d.dx(), d.dz());
        InventoryA inv = d.get(InventoryA.class);
        if (inv != null) inv.process(d.dx(), d.dz());
        NoFallA nf = d.get(NoFallA.class);
        if (nf != null) nf.process(d.dy(), g);
        JesusA js = d.get(JesusA.class);
        if (js != null) js.process(d.dx(), d.dz(), g);
        FlyA fly = d.get(FlyA.class);
        if (fly != null) fly.process(d.dy(), g);
    }

    private void rot(PlayerData d, float yaw, float pitch) {
        d.rot(yaw, pitch);
        BadPacketsA bp = d.get(BadPacketsA.class);
        if (bp != null) bp.process(d.x(), d.y(), d.z(), yaw, pitch, d.ground());
        AimA a = d.get(AimA.class);
        if (a != null) a.process(d.dyaw(), d.dpitch());
        AimB b = d.get(AimB.class);
        if (b != null) b.process(d.dyaw(), d.dpitch());
        AimC c = d.get(AimC.class);
        if (c != null) c.process(yaw, d.dyaw(), d.dx(), d.dz());
    }
}
