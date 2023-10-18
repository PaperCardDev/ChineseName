package cn.paper_card.chinese_name;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ChineseName extends JavaPlugin implements ChineseNameApi, Listener {

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull NameServiceImpl nameService;
    private final @NotNull ApplicationServiceImpl applicationService;

    private final @NotNull TextComponent prefix;

    public ChineseName() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.nameService = new NameServiceImpl(this);
        this.applicationService = new ApplicationServiceImpl(this);
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("中文名").color(NamedTextColor.AQUA))
                .append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE))
                .build();
    }

    @Override
    public void onEnable() {

        final PluginCommand command = this.getCommand("ch-name");
        final MyCommand myCommand = new MyCommand(this);
        assert command != null;
        command.setExecutor(myCommand);
        command.setTabCompleter(myCommand);

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.nameService.destroy();
        this.applicationService.destroy();
    }


    @Override
    public @NotNull NameService getNameService() {
        return this.nameService;
    }

    @Override
    public @NotNull ApplicationService getApplicationService() {
        return this.applicationService;
    }


    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {
        final Player player = event.getPlayer();

        final Component message = event.message();
        if (!(message instanceof final TextComponent textComponent)) return;

        final String content = textComponent.content();


        final String prefix = "中文名申请";
        if (!content.startsWith(prefix)) return;

        this.taskScheduler.runTaskAsynchronously(() -> {

            final String chName = content.substring(prefix.length());

            // 检查中文名是否合法
            try {
                this.getNameService().checkNameValid(chName);
            } catch (Exception e) {
                player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.DARK_RED));
                return;
            }

            // 检查名字是否被使用
            final NameInfo nameInfo;
            try {
                nameInfo = this.getNameService().queryByName(chName);
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(Component.text(e.toString()).color(NamedTextColor.DARK_RED));
                return;
            }

            if (nameInfo != null) {
                player.sendMessage(Component.text("""
                        该中文名 [%s] 已被注册使用，请申请其它的名字~""".formatted(chName)));
                return;
            }

            // 检查是否已经在申请了
            final ApplicationInfo applicationInfo;

            try {
                applicationInfo = this.getApplicationService().queryByName(chName);
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(Component.text(e.toString()).color(NamedTextColor.DARK_RED));
                return;
            }

            if (applicationInfo != null) {
                player.sendMessage(Component.text("""
                        该中文名 [%s] 已被申请，请申请其它的名字~"""
                        .formatted(chName)));
                return;
            }

            int id;

            try {
                id = this.getApplicationService().addOrUpdateByUuid(new ApplicationInfo(
                        0,
                        player.getUniqueId(),
                        chName,
                        System.currentTimeMillis()
                ));
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(Component.text(e.toString()).color(NamedTextColor.DARK_RED));
                return;
            }

            player.sendMessage(Component.text("""
                    %s申请成功，中文名：%s，请等待管理员审核~""".formatted(
                    id > 0 ? "添加" : "修改",
                    chName
            )));
        });
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build()
        );
    }

    void sendWarning(@NotNull CommandSender sender, @NotNull String warning) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(warning).color(NamedTextColor.YELLOW))
                .build()
        );
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull String info) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(info).color(NamedTextColor.GREEN))
                .build()
        );
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull TextComponent info) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(info)
                .build()
        );
    }

}
