package cn.paper_card.chinese_name;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

class MyUtil {
    static @NotNull TextComponent coinsNumber(long v) {
        return Component.text(v)
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    }
}
