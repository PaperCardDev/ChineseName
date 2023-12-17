package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ApplicationInfo;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class ApplicationTable {

    private static final String NAME = "chinese_name_app";

    private PreparedStatement statementQueryById = null;

    private PreparedStatement statementQueryByName = null;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementUpdateByUuid = null;

    private PreparedStatement statementDeleteById = null;

    private PreparedStatement statementQueryWithPage = null;

    private final @NotNull Connection connection;

    ApplicationTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.createTable();
    }

    private void createTable() throws SQLException {

        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    id  INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
                    uid1    BIGINT NOT NULL,
                    uid2    BIGINT NOT NULL,
                    name    VARCHAR(64) NOT NULL UNIQUE,
                    time    BIGINT NOT NULL
                )""".formatted(NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, time) VALUES (?, ?, ?, ?)".formatted(NAME),
                            Statement.RETURN_GENERATED_KEYS);
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementUpdateByUuid() throws SQLException {
        if (this.statementUpdateByUuid == null) {
            this.statementUpdateByUuid = this.connection.prepareStatement
                    ("UPDATE %s SET name=?,time=? WHERE uid1=? AND uid2=? LIMIT 1".formatted(NAME));
        }
        return this.statementUpdateByUuid;
    }

    private @NotNull PreparedStatement getStatementDeleteById() throws SQLException {
        if (this.statementDeleteById == null) {
            this.statementDeleteById = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE id=? LIMIT 1".formatted(NAME));
        }
        return this.statementDeleteById;
    }

    private @NotNull PreparedStatement getStatementQueryById() throws SQLException {
        if (this.statementQueryById == null) {
            this.statementQueryById = this.connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s WHERE id=? LIMIT 1".formatted(NAME));
        }
        return this.statementQueryById;
    }

    private @NotNull PreparedStatement getStatementQueryByName() throws SQLException {
        if (this.statementQueryByName == null) {
            this.statementQueryByName = this.connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s WHERE name=? LIMIT 1".formatted(NAME));
        }
        return this.statementQueryByName;
    }

    private @NotNull PreparedStatement getStatementQueryWithPage() throws SQLException {

        if (this.statementQueryWithPage == null) {
            this.statementQueryWithPage = this.connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s LIMIT ? OFFSET ?".formatted(NAME));
        }

        return this.statementQueryWithPage;
    }

    private int parseIds(@NotNull ResultSet resultSet) throws SQLException {

        final int id;
        try {
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            } else throw new SQLException("不应该没有数据！");

            if (resultSet.next()) throw new SQLException("不应该还有数据！");

        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }

            throw e;
        }

        resultSet.close();

        return id;
    }

    private @NotNull ApplicationInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final int id = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final String name = resultSet.getString(4);
        final long time = resultSet.getLong(5);
        return new ApplicationInfo(
                id,
                new UUID(uid1, uid2),
                name,
                time
        );
    }

    private @Nullable ApplicationInfo parseOne(@NotNull ResultSet resultSet) throws SQLException {

        final ApplicationInfo info;
        try {
            if (resultSet.next()) info = this.parseRow(resultSet);
            else info = null;

            if (resultSet.next()) throw new SQLException("不应该还有数据！");
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return info;
    }

    private @NotNull List<ApplicationInfo> parseAll(@NotNull ResultSet resultSet) throws SQLException {

        final LinkedList<ApplicationInfo> list = new LinkedList<>();
        try {
            while (resultSet.next()) list.add(this.parseRow(resultSet));
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }
        resultSet.close();

        return list;
    }

    int insert(@NotNull UUID uuid, @NotNull String name, long time) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setString(3, name);
        ps.setLong(4, time);

        ps.executeUpdate();

        final ResultSet generatedKeys = ps.getGeneratedKeys();

        return this.parseIds(generatedKeys);
    }

    int deleteById(int id) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteById();

        ps.setInt(1, id);

        return ps.executeUpdate();
    }


    int updateByUuid(@NotNull UUID uuid, @NotNull String name, long time) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdateByUuid();

        ps.setString(1, name);
        ps.setLong(2, time);
        ps.setLong(3, uuid.getMostSignificantBits());
        ps.setLong(4, uuid.getLeastSignificantBits());

        return ps.executeUpdate();
    }

    @Nullable ApplicationInfo queryById(int id) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryById();

        ps.setInt(1, id);

        final ResultSet resultSet = ps.executeQuery();

        return this.parseOne(resultSet);
    }

    @Nullable ApplicationInfo queryByName(@NotNull String name) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByName();

        ps.setString(1, name);

        final ResultSet resultSet = ps.executeQuery();

        return this.parseOne(resultSet);
    }

    @NotNull List<ApplicationInfo> queryWithPage(int limit, int offset) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryWithPage();

        ps.setInt(1, limit);
        ps.setInt(2, offset);

        final ResultSet resultSet = ps.executeQuery();

        return this.parseAll(resultSet);
    }
}
