package cn.paper_card.chinese_name;

import cn.paper_card.database.DatabaseApi;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NameServiceImpl implements ChineseNameApi.NameService {

    private Connection connection = null;
    private ChineseNameTable table = null;

    private final @NotNull Pattern compile;

    private final @NotNull ChineseName plugin;

    NameServiceImpl(@NotNull ChineseName plugin) {
        this.plugin = plugin;
        this.compile = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
    }

    private @NotNull Connection getConnection() throws Exception {
        if (this.connection == null) {
            final Plugin p = this.plugin.getServer().getPluginManager().getPlugin("Database");
            if (p instanceof final DatabaseApi api) {
                this.connection = api.connectImportant().getConnection();
            } else throw new Exception("Database插件未安装！");
        }
        return this.connection;
    }

    private @NotNull ChineseNameTable getTable() throws Exception {
        if (this.table == null) {
            this.table = new ChineseNameTable(this.getConnection());
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
    public void checkNameValid(@NotNull String name) throws Exception {
        final Matcher matcher = this.compile.matcher(name);
        if (!matcher.matches()) throw new Exception("不正确的中文名：%s，字数只能为2~4个字".formatted(name));
    }

    @Override
    public boolean addOrUpdateByUuid(ChineseNameApi.@NotNull NameInfo info) throws Exception {
        synchronized (this) {
            final ChineseNameTable t = this.getTable();

            final int updated = t.updateByUuid(info);
            if (updated == 0) {
                final int inserted = t.insert(info);
                if (inserted != 1) throw new Exception("插入了%d条数据！".formatted(inserted));
                return true;
            }

            if (updated == 1) return false;

            throw new Exception("根据一个UUID更新了%d条信息！".formatted(updated));
        }
    }

    @Override
    public @Nullable ChineseNameApi.NameInfo queryByUuid(@NotNull UUID uuid) throws Exception {
        synchronized (this) {
            final ChineseNameTable t = this.getTable();
            final List<ChineseNameApi.NameInfo> list = t.queryByUuid(uuid);
            final int size = list.size();
            if (size == 1) return list.get(0);
            if (size == 0) return null;
            throw new Exception("根据一个UUID查询到了%d条数据！".formatted(size));
        }
    }

    @Override
    public @Nullable ChineseNameApi.NameInfo queryByName(@NotNull String name) throws Exception {
        synchronized (this) {
            final ChineseNameTable t = this.getTable();
            final List<ChineseNameApi.NameInfo> list = t.queryByName(name);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) return list.get(0);
            throw new Exception("根据一个中文名[%s]查询到了%d条数据！".formatted(name, size));
        }
    }
}
