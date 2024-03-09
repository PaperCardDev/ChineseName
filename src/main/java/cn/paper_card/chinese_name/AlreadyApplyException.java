package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ApplicationInfo;
import org.jetbrains.annotations.NotNull;

public class AlreadyApplyException extends Exception {
    final @NotNull ApplicationInfo info;

    public AlreadyApplyException(@NotNull ApplicationInfo info, @NotNull String messge) {
        super(messge);
        this.info = info;
    }

    public @NotNull ApplicationInfo getInfo() {
        return this.info;
    }
}
