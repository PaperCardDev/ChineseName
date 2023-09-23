package cn.paper_card.chinese_name;

import cn.paper_card.database.DatabaseApi;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

class ApplicationServiceImpl implements ChineseNameApi.ApplicationService {

    private Connection connection = null;
    private ApplicationTable table = null;

    private final @NotNull ChineseName plugin;

    ApplicationServiceImpl(@NotNull ChineseName plugin) {
        this.plugin = plugin;
    }

    private @NotNull Connection getConnection() throws Exception {
        if (this.connection == null) {
            final Plugin p = this.plugin.getServer().getPluginManager().getPlugin("Database");
            if (p instanceof DatabaseApi api) {
                this.connection = api.connectUnimportant().getConnection();
            } else throw new Exception("Database插件未安装！");
        }
        return this.connection;
    }

    private @NotNull ApplicationTable getTable() throws Exception {
        if (this.table == null) {
            this.table = new ApplicationTable(this.getConnection());
        }
        return this.table;
    }

    void destroy() {
        synchronized (this) {
            if (this.table != null) {
                try {
                    this.table.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe(e.toString());
                    e.printStackTrace();
                }
                this.table = null;
            }

            if (this.connection != null) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe(e.toString());
                    e.printStackTrace();
                }
                this.connection = null;
            }
        }

    }

    @Override
    public int addOrUpdateByUuid(@NotNull ChineseNameApi.ApplicationInfo info) throws Exception {
        synchronized (this) {
            final ApplicationTable t = this.getTable();
            final int updated = t.updateByUuid(info.uuid(), info.name(), info.time());
            if (updated == 1) return 0;
            if (updated == 0) {
                final List<Integer> ids = t.insert(info.uuid(), info.name(), info.time());
                final int size = ids.size();
                if (size == 1) return ids.get(0);
                if (size == 0) return -1;
                throw new Exception("插入数据生成了%d个ID！".formatted(size));
            }
            throw new Exception("根据一个UUID更新了%d条数据！".formatted(updated));
        }
    }

    @Override
    public @Nullable ChineseNameApi.ApplicationInfo queryByName(@NotNull String name) throws Exception {
        synchronized (this) {
            final ApplicationTable t = this.getTable();
            final List<ChineseNameApi.ApplicationInfo> list = t.queryByName(name);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) return list.get(0);
            throw new Exception("根据一个中文名[%s]查询到了%d条数据！".formatted(name, size));
        }
    }

    @Override
    public @NotNull List<ChineseNameApi.ApplicationInfo> queryWithLimit(int limit) throws Exception {
        synchronized (this) {
            final ApplicationTable t = this.getTable();
            return t.queryWithLimit(limit);
        }
    }

    @Override
    public @Nullable ChineseNameApi.ApplicationInfo takeById(int id) throws Exception {
        synchronized (this) {
            final ApplicationTable t = this.getTable();
            final List<ChineseNameApi.ApplicationInfo> list = t.queryById(id);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) {
                final ChineseNameApi.ApplicationInfo applicationInfo = list.get(0);

                // 删除
                final int deleted = t.deleteById(id);
                if (deleted != 1) throw new Exception("根据一个ID删除了%d条数据！".formatted(deleted));

                return applicationInfo;
            }
            throw new Exception("根据一个申请ID查询到了%d条数据！".formatted(size));
        }
    }
}
