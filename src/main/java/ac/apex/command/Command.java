package ac.apex.command;

import ac.apex.Apex;
import ac.apex.data.PlayerData;
import ac.apex.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Command implements CommandExecutor, TabCompleter {
    private final Apex plugin;

    public Command(Apex plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("apex.admin") && !sender.hasPermission("apex.alerts")) {
            sender.sendMessage(Chat.color("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "alerts": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Chat.color("&cPlayer only command."));
                    return true;
                }
                Player player = (Player) sender;
                boolean state = plugin.toggleAlerts(player);
                sender.sendMessage(Chat.color(String.format(
                        "&8[&b&lAPEX&8] &7Alerts have been %s&7.", state ? "&aenabled" : "&cdisabled")));
                return true;
            }

            case "verbose": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Chat.color("&cPlayer only command."));
                    return true;
                }
                Player player = (Player) sender;
                boolean state = plugin.toggleVerbose(player);
                sender.sendMessage(Chat.color(String.format(
                        "&8[&b&lAPEX&8] &7Verbose debug has been %s&7.", state ? "&aenabled" : "&cdisabled")));
                return true;
            }

            case "info":
            case "inspect": {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&cUsage: /apex info <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&cPlayer not found."));
                    return true;
                }
                PlayerData profile = plugin.data().get(target);
                sendPlayerInfo(sender, profile);
                return true;
            }

            case "ping": {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&cUsage: /apex ping <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&cPlayer not found."));
                    return true;
                }
                PlayerData profile = plugin.data().get(target);
                long ping = profile.ping().ms();
                sender.sendMessage(Chat.color(String.format(
                        "&8[&b&lAPEX&8] &b%s&7's Ping: &f%d ms &8(&7Synced via Transactions&8)", target.getName(), ping)));
                return true;
            }

            case "ban": {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&cUsage: /apex ban <player> [time] [reason]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&cPlayer must be online."));
                    return true;
                }
                String duration = args.length > 2 ? args[2] : "30d";
                String reason = args.length > 3
                        ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : "Unfair Advantage";

                plugin.punish().execute(target, reason, "Manual Ban", duration);
                sender.sendMessage(Chat.color(String.format(
                        "&8[&b&lAPEX&8] &aSuspended &b%s &afor &e%s &8(&f%s&8).",
                        target.getName(), duration, reason)));
                return true;
            }

            case "unban": {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&cUsage: /apex unban <player>"));
                    return true;
                }
                boolean unbanned = plugin.punish().unban(args[1]);
                if (unbanned) {
                    sender.sendMessage(Chat.color(String.format(
                            "&8[&b&lAPEX&8] &aUnbanned &b%s&a.", args[1])));
                } else {
                    sender.sendMessage(Chat.color("&cNo active suspension found."));
                }
                return true;
            }

            case "reload": {
                plugin.reloadConfig();
                try { plugin.punish().reloadCache(); } catch (Throwable ignored) {}
                sender.sendMessage(Chat.color("&8[&b&lAPEX&8] &aConfiguration reloaded. &7(&fban-screen cached&7)"));
                return true;
            }

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Chat.color(
                "&8&m--------------------------------------------------\n" +
                " &#00f2fe&lAPEX ANTI-CHEAT &7• Build v1.0.0\n" +
                "&8&m--------------------------------------------------\n" +
                " &7• &#00f2fe/apex alerts &8- &7Toggle live detection alerts\n" +
                " &7• &#00f2fe/apex verbose &8- &7Toggle real-time debug stream\n" +
                " &7• &#00f2fe/apex info <player> &8- &7View player telemetry & checks\n" +
                " &7• &#00f2fe/apex ping <player> &8- &7View transaction latency\n" +
                " &7• &#00f2fe/apex ban <player> [time] [reason] &8- &7Ban player\n" +
                " &7• &#00f2fe/apex unban <player> &8- &7Lift suspension\n" +
                " &7• &#00f2fe/apex reload &8- &7Reload configuration\n" +
                "&8&m--------------------------------------------------"
        ));
    }

    private void sendPlayerInfo(CommandSender sender, PlayerData p) {
        sender.sendMessage(Chat.color(String.format(
                "&8&m--------------------------------------------------\n" +
                " &#00f2fe&lTELEMETRY PROFILE &8[&f%s&8]\n" +
                "&8&m--------------------------------------------------\n" +
                " &7• Client Version: &f%s\n" +
                " &7• Transaction Ping: &b%d ms\n" +
                " &7• Position: &f(%.2f, %.2f, %.2f)\n" +
                " &7• Ground State: &f%s &8(Last: %s)\n" +
                " &7• Status: &f%s &8| &f%s\n" +
                "&8&m--------------------------------------------------",
                p.name(),
                p.version().name(),
                p.ping().ms(),
                p.x(), p.y(), p.z(),
                p.ground() ? "&aOnGround" : "&cInAir",
                p.lastGround() ? "&aOnGround" : "&cInAir",
                p.sprint() ? "&bSprinting" : "&7Walking",
                p.sneak() ? "&eSneaking" : "&7Standing"
        )));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("alerts", "verbose", "info", "ping", "ban", "unban", "reload", "help");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (Arrays.asList("info", "inspect", "ping", "ban", "unban").contains(args[0].toLowerCase())) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("ban")) {
            return Arrays.asList("7d", "14d", "30d", "90d", "perm");
        }
        return Collections.emptyList();
    }
}
