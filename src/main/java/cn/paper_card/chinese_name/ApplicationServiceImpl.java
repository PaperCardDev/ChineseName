package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ApplicationInfo;
import cn.paper_card.chinese_name.api.ApplicationService;
import cn.paper_card.chinese_name.api.NameInfo;
import cn.paper_card.chinese_name.api.exception.InvalidNameException;
import cn.paper_card.chinese_name.api.exception.NameAppliedException;
import cn.paper_card.chinese_name.api.exception.NameRegisteredException;
import cn.paper_card.database.api.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ApplicationServiceImpl implements ApplicationService {

    interface NameChecker {
        @Nullable NameInfo queryByName(@NotNull String name) throws SQLException;
    }


    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    private Connection connection = null;
    private ApplicationTable table = null;

    private final @NotNull NameChecker nameChecker;

    private final @NotNull Pattern compile;


    ApplicationServiceImpl(@NotNull DatabaseApi.MySqlConnection mySqlConnection, @NotNull NameChecker nameChecker) {
        this.mySqlConnection = mySqlConnection;
        this.nameChecker = nameChecker;

        this.compile = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
    }

    private @NotNull ApplicationTable getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == newCon) return this.table;

        if (this.table != null) this.table.close();
        this.table = new ApplicationTable(newCon);
        this.connection = newCon;
        return this.table;
    }

    void destroy() throws SQLException {
        synchronized (this.mySqlConnection) {
            final ApplicationTable t = this.table;

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
    public void checkNameValid(@NotNull String name) throws InvalidNameException {
        final Matcher matcher = this.compile.matcher(name);
        if (!matcher.matches())
            throw new InvalidNameException("不正确的中文名：%s，字数只能为2~4个字，只能为汉字".formatted(name));
    }

    @Override
    public int addOrUpdateByUuid(@NotNull ApplicationInfo info) throws NameRegisteredException, NameAppliedException, InvalidNameException, SQLException {

        // 检查名字
        this.checkNameValid(info.name());

        synchronized (this.mySqlConnection) {
            try {
                final ApplicationTable t = this.getTable();

                // 查询是否已经被注册
                final NameInfo nameInfo = this.nameChecker.queryByName(info.name());
                if (nameInfo != null)
                    throw new NameRegisteredException(nameInfo, "中文名 %s 已经被注册！".formatted(info.name()));

                // 查询是否已经被申请
                final ApplicationInfo info1 = t.queryByName(info.name());
                this.mySqlConnection.setLastUseTime();

                if (info1 != null)
                    throw new NameAppliedException(info1, "中文名 %s 已经被申请！".formatted(info1.name()));

                // 更新
                final int updated = t.updateByUuid(info.uuid(), info.name(), info.time());
                this.mySqlConnection.setLastUseTime();

                // 插入
                if (updated == 0) {
                    final int id = t.insert(info.uuid(), info.name(), info.time());
                    this.mySqlConnection.setLastUseTime();
                    return id;
                }

                if (updated == 1) return 0;

                throw new RuntimeException("更新了%d条数据！".formatted(updated));
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
    public @Nullable ApplicationInfo queryByName(@NotNull String name) throws SQLException {
        synchronized (this.mySqlConnection) {

            try {
                final ApplicationTable t = this.getTable();

                final ApplicationInfo info = t.queryByName(name);
                this.mySqlConnection.setLastUseTime();

                return info;
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
    public @NotNull List<ApplicationInfo> queryWithPage(int limit, int offset) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final List<ApplicationInfo> list;

                final ApplicationTable t = this.getTable();

                list = t.queryWithPage(limit, offset);
                this.mySqlConnection.setLastUseTime();

                return list;
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
    public @Nullable ApplicationInfo takeById(int id) throws SQLException {
        synchronized (this.mySqlConnection) {

            try {
                final ApplicationTable t = this.getTable();

                final ApplicationInfo info = t.queryById(id);
                this.mySqlConnection.setLastUseTime();

                if (info == null) return null;

                final int deleted = t.deleteById(id);

                if (deleted != 1) throw new RuntimeException("删除了%d条数据！".formatted(deleted));

                return info;

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
