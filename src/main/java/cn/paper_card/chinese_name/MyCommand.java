package cn.paper_card.chinese_name;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        this.addSubCommand(new App());
        this.addSubCommand(new AppList());
        this.addSubCommand(new Accept());
        this.addSubCommand(new Reject());
        this.addSubCommand(new UseOnOff(true));
        this.addSubCommand(new UseOnOff(false));
        this.addSubCommand(new Help());
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
                plugin.sendError(commandSender, "你必须指定参数：玩家名或UUID");
                return true;
            }

            if (argNewName == null) {
                plugin.sendError(commandSender, "你必须指定参数：新的中文名");
                return true;
            }

            final UUID uuid = parseArgPlayer(argPlayer);

            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
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
                    plugin.sendError(commandSender, e.toString());
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
                    final TextComponent.Builder append = Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 已将你的中文名设置为：[").color(NamedTextColor.GREEN))
                            .append(Component.text(argNewName).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，下次连接服务器就生效啦~").color(NamedTextColor.GREEN));

                    plugin.sendInfo(player, append.build());
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

                final int size = list.size();

                final TextComponent.Builder builder = Component.text();
                builder.append(Component.text("---- 中文名申请列表 ----"));
                builder.appendNewline();


                if (size == 0) {
                    builder.append(Component.text("当前没有任何中文名申请").color(NamedTextColor.GRAY));
                    builder.appendNewline();
                } else {
                    final SimpleDateFormat format = new SimpleDateFormat("MM月dd日，HH:mm");

                    builder.append(Component.text("ID | 中文名 | 原名 | 时间 | 操作").color(NamedTextColor.GRAY));
                    builder.appendNewline();

                    for (ChineseNameApi.ApplicationInfo info : list) {

                        builder.append(Component.text(info.id()));
                        builder.append(Component.text(" | "));

                        builder.append(Component.text(info.name()));
                        builder.append(Component.text(" | "));

                        final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(info.uuid());
                        String name = offlinePlayer.getName();
                        if (name == null) name = offlinePlayer.getUniqueId().toString();

                        builder.append(Component.text(name));
                        builder.append(Component.text(" | "));

                        builder.append(Component.text(format.format(info.time())));
                        builder.append(Component.text(" | "));

                        builder.append(Component.text("[同意]")
                                .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.runCommand("/ch-name accept %d".formatted(info.id())))
                        );
                        builder.appendSpace();

                        builder.append(Component.text("[拒绝]")
                                .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.runCommand("/ch-name reject %d".formatted(info.id())))
                        );

                        builder.appendNewline();
                    }
                }
                builder.append(Component.text("========").color(NamedTextColor.GREEN));

                plugin.sendInfo(commandSender, builder.build());
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
                plugin.sendError(commandSender, "你必须提供参数：申请ID");
                return true;
            }

            final int id;

            try {
                id = Integer.parseInt(argId);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的申请！".formatted(argId));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final ChineseNameApi.ApplicationInfo info;

                try {
                    info = plugin.getApplicationService().takeById(id);
                } catch (Exception e) {
                    plugin.sendError(commandSender, e.toString());
                    e.printStackTrace();
                    return;
                }

                if (info == null) {
                    plugin.sendError(commandSender, "以%d为ID的申请不存在！".formatted(id));
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
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(info.uuid());
                String name = offlinePlayer.getName();
                if (name == null) name = offlinePlayer.getUniqueId().toString();

                plugin.sendInfo(commandSender, "%s了玩家%s的中文名: %s".formatted(
                        added ? "添加" : "更新",
                        name, info.name()
                ));


                // 如果玩家在线，通知玩家
                final Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    final TextComponent.Builder append = Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 已同意你的中文名申请 [").color(NamedTextColor.GREEN))
                            .append(Component.text(info.name()).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，下次连接服务器就生效啦~").color(NamedTextColor.GREEN));

                    plugin.sendInfo(player, append.build());
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
                plugin.sendError(commandSender, "你必须提供参数：申请ID");
                return true;
            }

            final int id;

            try {
                id = Integer.parseInt(argId);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的申请！".formatted(argId));
                return true;
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final ChineseNameApi.ApplicationInfo info;

                // 取出
                try {
                    info = plugin.getApplicationService().takeById(id);
                } catch (Exception e) {
                    plugin.sendError(commandSender, e.toString());
                    e.printStackTrace();
                    return;
                }

                if (info == null) {
                    plugin.sendError(commandSender, "以%d为ID的申请不存在！".formatted(id));
                    return;
                }


                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(info.uuid());
                String name = offlinePlayer.getName();
                if (name == null) name = offlinePlayer.getUniqueId().toString();


                plugin.sendInfo(commandSender, "拒绝了玩家%s的中文名: %s".formatted(
                        name, info.name()
                ));

                // 如果玩家在线，通知玩家
                final Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    final TextComponent.Builder append = Component.text()
                            .append(Component.text("管理员 [").color(NamedTextColor.GREEN))
                            .append(Component.text(commandSender.getName()).color(NamedTextColor.DARK_RED))
                            .append(Component.text("] 拒绝了你的中文名申请 [").color(NamedTextColor.GREEN))
                            .append(Component.text(info.name()).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]，可以换个别的名字重新申请~").color(NamedTextColor.GREEN));

                    plugin.sendInfo(player, append.build());

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

    class App extends TheMcCommand {

        private final @NotNull Permission permission;

        protected App() {
            super("app");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argNewName = strings.length > 0 ? strings[0] : null;
            if (argNewName == null) {
                plugin.sendError(commandSender, "你必须提供参数：要使用的中文名");
                return true;
            }

            if (strings.length != 1) {
                plugin.sendWarning(commandSender, "参数数量不太对，只需要一个参数，而你提供了%d个参数，是不是中文名里包含空格？".formatted(strings.length));
                return true;
            }

            if (!(commandSender instanceof final Player player)) {
                plugin.sendError(commandSender, "该命令只能由玩家来执行");
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                // 检查中文名是否合法
                try {
                    plugin.getNameService().checkNameValid(argNewName);
                } catch (Exception e) {
                    plugin.sendWarning(commandSender, e.getMessage());
                    return;
                }

                // 检查名字是否被使用
                final ChineseNameApi.NameInfo nameInfo;
                try {
                    nameInfo = plugin.getNameService().queryByName(argNewName);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (nameInfo != null) {
                    plugin.sendWarning(commandSender, """
                            该中文名 [%s] 已被注册使用，请申请其它的名字~""".formatted(argNewName));
                    return;
                }

                // 检查是否已经在申请了
                final ChineseNameApi.ApplicationInfo applicationInfo;

                try {
                    applicationInfo = plugin.getApplicationService().queryByName(argNewName);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (applicationInfo != null) {
                    plugin.sendWarning(commandSender, """
                            该中文名 [%s] 已被申请，请申请其它的名字~"""
                            .formatted(argNewName));
                    return;
                }

                int id;

                try {
                    id = plugin.getApplicationService().addOrUpdateByUuid(new ChineseNameApi.ApplicationInfo(
                            0,
                            player.getUniqueId(),
                            argNewName,
                            System.currentTimeMillis()
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(player, e.toString());
                    return;
                }

                plugin.sendInfo(commandSender, """
                        %s申请成功，中文名：%s，请等待管理员审核~""".formatted(
                        id > 0 ? "添加" : "修改",
                        argNewName
                ));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String arg = strings[0];
                if (arg.isEmpty()) {
                    final LinkedList<String> list = new LinkedList<>();
                    list.add("<中文名>");
                    return list;
                }
            }

            return null;
        }
    }

    class UseOnOff extends TheMcCommand {

        private final boolean isOn;

        private final @NotNull Permission permission;

        protected UseOnOff(boolean isOn) {
            super(isOn ? "use-on" : "use-off");
            this.isOn = isOn;
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (!(commandSender instanceof final Player player)) {
                plugin.sendError(commandSender, "该命令只能由玩家来执行");
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final ChineseNameApi.NameInfo nameInfo;

                try {
                    nameInfo = plugin.getNameService().queryByUuid(player.getUniqueId());
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (nameInfo == null) {
                    plugin.sendWarning(commandSender, "你没有注册中文名");
                    return;
                }

                if (nameInfo.inUse() == this.isOn) {
                    final String status = nameInfo.inUse() ? "启用" : "禁用";
                    plugin.sendWarning(commandSender, "你的中文名已经是 [" + status + "] 状态");
                    return;
                }

                final ChineseNameApi.NameInfo newInfo = new ChineseNameApi.NameInfo(
                        player.getUniqueId(),
                        nameInfo.name(),
                        System.currentTimeMillis(),
                        this.isOn
                );

                final boolean added;

                try {
                    added = plugin.getNameService().addOrUpdateByUuid(newInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                plugin.sendInfo(player, "%s成功，已将你的中文名 [%s] 设置为 %s，重新连接服务器生效~".formatted(
                        added ? "添加" : "更新", newInfo.name(), this.isOn ? "启用" : "禁用"
                ));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

    class Help extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Help() {
            super("help");
            this.permission = plugin.addPermission(MyCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String link = "https://docs.qq.com/doc/DV2l0SlJrbmRldFhH";
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("点击打开帮助文档：").color(NamedTextColor.GREEN))
                    .append(Component.text(link)
                            .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(link)))
                    .build());

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }
}
