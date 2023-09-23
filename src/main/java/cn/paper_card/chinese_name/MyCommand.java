package cn.paper_card.chinese_name;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

class MyCommand extends TheMcCommand.HasSub {

    private final @NotNull ChineseName plugin;
    private final @NotNull Permission permission;


    MyCommand(@NotNull ChineseName plugin) {
        super("ch-name");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("ch-name.command"));

        this.addSubCommand(new Set());
        this.addSubCommand(new AppList());
        this.addSubCommand(new Accept());
        this.addSubCommand(new Reject());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    private @Nullable UUID parseArgPlayer(@NotNull String argPlayer) {
        try {
            return UUID.fromString(argPlayer);
        } catch (IllegalArgumentException ignored) {
        }

        for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
            final String name = offlinePlayer.getName();
            if (argPlayer.equals(name)) return offlinePlayer.getUniqueId();
        }
        return null;
    }

    private static void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text(error).color(NamedTextColor.DARK_RED));
    }

    class Set extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Set() {
            super("set");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + ".set");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argPlayer = strings.length > 0 ? strings[0] : null;
            final String argNewName = strings.length > 1 ? strings[1] : null;

            if (argPlayer == null) {
                sendError(commandSender, "你必须指定参数：玩家名或UUID");
                return true;
            }

            if (argNewName == null) {
                sendError(commandSender, "你必须指定参数：新的中文名");
                return true;
            }

            final UUID uuid = parseArgPlayer(argPlayer);

            if (uuid == null) {
                sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                boolean added;
                try {
                    added = plugin.getNameService().addOrUpdateByUuid(new ChineseNameApi.NameInfo(
                            uuid,
                            argNewName,
                            System.currentTimeMillis(),
                            true
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendError(commandSender, e.toString());
                    return;
                }


                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName();
                if (playerName == null) playerName = offlinePlayer.getUniqueId().toString();

                commandSender.sendMessage(Component.text("%s成功，已将玩家 [%s] 的中文名设置为: %s".formatted(
                        added ? "添加" : "更新", playerName, argNewName
                )));

                // 如果玩家在线通知玩家
                final Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 已将你的中文名设置为：[").color(NamedTextColor.GREEN))
                            .append(Component.text(argNewName).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，下次连接服务器就生效啦~").color(NamedTextColor.GREEN))
                    );

                }
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String argPlayer = strings[0];
                final LinkedList<String> list = new LinkedList<>();

                if (argPlayer.isEmpty()) list.add("<玩家名或UUID>");

                for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                    final String name = offlinePlayer.getName();
                    if (name == null) continue;
                    if (name.startsWith(argPlayer)) list.add(name);
                }

                return list;
            }

            if (strings.length == 2) {
                final String argNewName = strings[1];
                final LinkedList<String> list = new LinkedList<>();

                if (argNewName.isEmpty()) list.add("<新的中文名>");

                return list;
            }

            return null;
        }
    }

    class AppList extends TheMcCommand {

        private final Permission permission;

        protected AppList() {
            super("app-list");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + ".app-list");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final List<ChineseNameApi.ApplicationInfo> list;

                try {
                    list = plugin.getApplicationService().queryWithLimit(4);
                } catch (Exception e) {
                    e.printStackTrace();
                    commandSender.sendMessage(Component.text(e.toString()).color(NamedTextColor.DARK_RED));
                    return;
                }

                if (list.size() == 0) {
                    commandSender.sendMessage(Component.text("当前没有任何中文名申请"));
                    return;
                }

                final SimpleDateFormat format = new SimpleDateFormat("MM月dd日，HH:mm");

                final TextComponent.Builder text = Component.text();
                text.append(Component.text("ID | 中文名 | 原名 | 时间"));
                for (ChineseNameApi.ApplicationInfo applicationInfo : list) {

                    final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(applicationInfo.uuid());
                    String name = offlinePlayer.getName();
                    if (name == null) name = offlinePlayer.getUniqueId().toString();


                    text.append(Component.newline());
                    text.append(Component.text("%d | %s | %s | %s".formatted(
                            applicationInfo.id(),
                            applicationInfo.name(),
                            name,
                            format.format(applicationInfo.time())
                    )));
                }
                commandSender.sendMessage(text.build());
            });


            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

    class Accept extends TheMcCommand {

        private final Permission permission;

        protected Accept() {
            super("accept");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + ".accept");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argId = strings.length > 0 ? strings[0] : null;

            if (argId == null) {
                sendError(commandSender, "你必须提供参数：申请ID");
                return true;
            }

            final int id;

            try {
                id = Integer.parseInt(argId);
            } catch (NumberFormatException e) {
                sendError(commandSender, "%s 不是正确的申请！".formatted(argId));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final ChineseNameApi.ApplicationInfo info;

                try {
                    info = plugin.getApplicationService().takeById(id);
                } catch (Exception e) {
                    sendError(commandSender, e.toString());
                    e.printStackTrace();
                    return;
                }

                if (info == null) {
                    sendError(commandSender, "以%d为ID的申请不存在！".formatted(id));
                    return;
                }

                boolean added;

                try {
                    added = plugin.getNameService().addOrUpdateByUuid(new ChineseNameApi.NameInfo(
                            info.uuid(),
                            info.name(),
                            System.currentTimeMillis(),
                            true
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendError(commandSender, e.toString());
                    return;
                }

                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(info.uuid());
                String name = offlinePlayer.getName();
                if (name == null) name = offlinePlayer.getUniqueId().toString();

                commandSender.sendMessage(Component.text("%s了玩家%s的中文名: %s".formatted(
                        added ? "添加" : "更新",
                        name, info.name()
                )));

                // 如果玩家在线，通知玩家
                final Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 已同意你的中文名申请 [").color(NamedTextColor.GREEN))
                            .append(Component.text(info.name()).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，下次连接服务器就生效啦~").color(NamedTextColor.GREEN))
                    );
                }
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String arg = strings[0];
                final LinkedList<String> list = new LinkedList<>();
                if (arg.isEmpty()) list.add("<申请ID>");
                return list;
            }

            return null;
        }
    }

    class Reject extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Reject() {
            super("reject");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + ".reject");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argId = strings.length > 0 ? strings[0] : null;

            if (argId == null) {
                sendError(commandSender, "你必须提供参数：申请ID");
                return true;
            }

            final int id;

            try {
                id = Integer.parseInt(argId);
            } catch (NumberFormatException e) {
                sendError(commandSender, "%s 不是正确的申请！".formatted(argId));
                return true;
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final ChineseNameApi.ApplicationInfo info;

                // 取出
                try {
                    info = plugin.getApplicationService().takeById(id);
                } catch (Exception e) {
                    sendError(commandSender, e.toString());
                    e.printStackTrace();
                    return;
                }

                if (info == null) {
                    sendError(commandSender, "以%d为ID的申请不存在！".formatted(id));
                    return;
                }


                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(info.uuid());
                String name = offlinePlayer.getName();
                if (name == null) name = offlinePlayer.getUniqueId().toString();

                commandSender.sendMessage(Component.text("拒绝了玩家%s的中文名: %s".formatted(
                        name, info.name()
                )));

                // 如果玩家在线，通知玩家
                final Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 拒绝了你的中文名申请 [").color(NamedTextColor.GREEN))
                            .append(Component.text(info.name()).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，可以换个别的名字重新申请~").color(NamedTextColor.GREEN))
                    );
                }
            });


            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String arg = strings[0];
                final LinkedList<String> list = new LinkedList<>();
                if (arg.isEmpty()) list.add("<申请ID>");
                return list;
            }

            return null;
        }
    }
}
