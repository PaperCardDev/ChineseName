package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ChineseNameApi;
import cn.paper_card.chinese_name.api.NameInfo;
import cn.paper_card.database.api.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

class ChineseNameApiImpl implements ChineseNameApi {

    private final @NotNull NameServiceImpl nameService;

    private final @NotNull ApplicationServiceImpl applicationService;

    ChineseNameApiImpl(@NotNull DatabaseApi.MySqlConnection important) {

        this.nameService = new NameServiceImpl(important);

        this.applicationService = new ApplicationServiceImpl(important, new ApplicationServiceImpl.NameChecker() {
            @Override
            public @Nullable NameInfo queryByName(@NotNull String name) throws SQLException {
                return nameService.queryByName(name);
            }
        });
    }

    @Override
    public @NotNull NameServiceImpl getNameService() {
        return this.nameService;
    }

    @Override
    public @NotNull ApplicationServiceImpl getApplicationService() {
        return this.applicationService;
    }
}
