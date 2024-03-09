package cn.paper_card.chinese_name;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

class OnOpJoin implements Listener {

    private final @NotNull ThePlugin plugin;

    OnOpJoin(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        final MyCommand cmd = plugin.getMyCommand();
        if (cmd == null) return;

        if (!cmd.canHandleApp(player)) return;

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            final ChineseNameApiImpl api = plugin.getChineseNameApi();
            if (api == null) return;

            final int count;

            try {
                count = api.getApplicationService().queryCount();
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("", e);
                plugin.sendException(player, e);
                return;
            }

            if (count > 0) {
                final TextComponent.Builder text = Component.text();
                plugin.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("当前有"));
                text.append(Component.text(count).color(NamedTextColor.RED));
                text.append(Component.text("个中文名申请未处理 "));
                text.append(Component.text("[点击查看]")
                        .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/ch-name app-list"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击查看")))
                );

                player.sendMessage(text.build().color(NamedTextColor.YELLOW));
            }
        });


    }
}
