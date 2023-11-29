package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ChineseNameApi;
import cn.paper_card.database.api.DatabaseApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class ThePlugin extends JavaPlugin {

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull TextComponent prefix;

    private ChineseNameApiImpl chineseNameApi = null;


    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);

        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("中文名").color(NamedTextColor.AQUA))
                .append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE))
                .build();
    }

    void handleException(@NotNull String msg, @NotNull Throwable e) {
        this.getSLF4JLogger().error(msg, e);
    }

    @Override
    public void onLoad() {
        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("无法连接到" + DatabaseApi.class.getSimpleName());

        this.chineseNameApi = new ChineseNameApiImpl(api.getRemoteMySQL().getConnectionImportant());

        this.getSLF4JLogger().info("注册%s...".formatted(ChineseNameApi.class.getSimpleName()));
        this.getServer().getServicesManager().register(ChineseNameApi.class, this.chineseNameApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        new MyCommand(this);
    }

    @Override
    public void onDisable() {

        this.getServer().getServicesManager().unregisterAll(this);

        if (this.chineseNameApi != null) {
            try {
                this.chineseNameApi.getNameService().destroy();
            } catch (SQLException e) {
                this.handleException("destroy name service", e);
            }

            try {
                this.chineseNameApi.getApplicationService().destroy();
            } catch (SQLException e) {
                this.handleException("destroy application service", e);
            }
        }
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull ChineseNameApiImpl getChineseNameApi() {
        return this.chineseNameApi;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build()
        );
    }

    void sendException(@NotNull CommandSender sender, @NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();
        text.append(this.prefix);
        text.appendSpace();

        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }
        sender.sendMessage(text.build());
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

    void broadcast(@NotNull TextComponent info) {
        this.getServer().broadcast(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(info)
                .build());
    }
}
