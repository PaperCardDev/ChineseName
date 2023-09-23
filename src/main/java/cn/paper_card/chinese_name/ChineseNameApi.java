package cn.paper_card.chinese_name;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ChineseNameApi {
    record NameInfo(
            UUID uuid,
            String name,
            long time,
            boolean inUse
    ) {
    }


    record ApplicationInfo(
            int id,
            UUID uuid,
            String name,
            long time
    ) {
    }

    interface NameService {

        // 检查是否为合法中文名
        void checkNameValid(@NotNull String name) throws Exception;

        // 添加或更新中文名
        boolean addOrUpdateByUuid(@NotNull NameInfo info) throws Exception;

        // 根据UUID进行查询
        @SuppressWarnings("unused")
        @Nullable NameInfo queryByUuid(@NotNull UUID uuid) throws Exception;

        @Nullable NameInfo queryByName(@NotNull String name) throws Exception;

    }

    interface ApplicationService {
        int addOrUpdateByUuid(@NotNull ApplicationInfo info) throws Exception;

        @Nullable ApplicationInfo queryByName(@NotNull String name) throws Exception;

        @NotNull List<ApplicationInfo> queryWithLimit(int limit) throws Exception;

        @Nullable ApplicationInfo takeById(int id) throws Exception;

    }


    @NotNull NameService getNameService();

    @NotNull ApplicationService getApplicationService();

}
