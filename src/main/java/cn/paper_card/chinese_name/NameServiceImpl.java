package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.NameInfo;
import cn.paper_card.chinese_name.api.NameService;
import cn.paper_card.chinese_name.api.exception.NameRegisteredException;
import cn.paper_card.database.api.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

class NameServiceImpl implements NameService {

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    private Connection connection = null;

    private ChineseNameTable table = null;


    NameServiceImpl(@NotNull DatabaseApi.MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;

    }


    private @NotNull ChineseNameTable getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == newCon) return this.table;

        if (this.table != null) this.table.close();
        this.connection = newCon;
        this.table = new ChineseNameTable(newCon);
        return this.table;
    }

    void destroy() throws SQLException {
        synchronized (this.mySqlConnection) {
            final ChineseNameTable t = this.table;

            if (t == null) {
                this.connection = null;
                return;
            }

            this.connection = null;
            this.table = null;

            t.close();
        }
    }


    @Override
    public boolean addOrUpdateByUuid(@NotNull NameInfo info) throws SQLException, NameRegisteredException {
        synchronized (this.mySqlConnection) {

            try {
                final ChineseNameTable t = this.getTable();

                // 检查名字是否被注册
                final NameInfo i = t.queryByName(info.name());
                this.mySqlConnection.setLastUseTime();

                if (i != null) {
                    // 检查是不是自己
                    if (!i.uuid().equals(info.uuid())) {
                        throw new NameRegisteredException(i, "中文名[%s] 已被注册".formatted(i.name()));
                    }
                }

                // 更新
                final int updated = t.updateByUuid(info);
                this.mySqlConnection.setLastUseTime();

                if (updated == 0) {
                    final int inserted = t.insert(info);
                    this.mySqlConnection.setLastUseTime();

                    if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));
                    return true;
                }

                if (updated == 1) return false;

                throw new RuntimeException("根据一个UUID更新了%d条信息！".formatted(updated));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean removeName(@NotNull UUID uuid) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final ChineseNameTable t = this.getTable();

                final int deleted = t.deleteByUuid(uuid);
                this.mySqlConnection.setLastUseTime();

                if (deleted == 1) return true;
                if (deleted == 0) return false;

                throw new RuntimeException("删除了%d条数据！".formatted(deleted));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean toggleEnable(@NotNull UUID uuid, boolean enable) {
        throw new UnsupportedOperationException("懒得实现，好像没有必要");
    }

    @Override
    public @Nullable NameInfo queryByUuid(@NotNull UUID uuid) throws SQLException {
        synchronized (this.mySqlConnection) {

            try {
                final ChineseNameTable t = this.getTable();

                final NameInfo nameInfo = t.queryByUuid(uuid);
                this.mySqlConnection.setLastUseTime();

                return nameInfo;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable NameInfo queryByName(@NotNull String name) throws SQLException {
        synchronized (this.mySqlConnection) {

            try {
                final ChineseNameTable t;
                t = this.getTable();

                final NameInfo nameInfo = t.queryByName(name);
                this.mySqlConnection.setLastUseTime();

                return nameInfo;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }
}
