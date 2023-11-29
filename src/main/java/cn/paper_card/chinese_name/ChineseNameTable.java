package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.NameInfo;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

class ChineseNameTable {

    private static final String TABLE_NAME = "chinese_name";

    private PreparedStatement statementQueryByUuid = null;
    private PreparedStatement statementQueryByName = null;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementDelete = null;

    private PreparedStatement statementUpdate = null;

    private final @NotNull Connection connection;

    ChineseNameTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.createTable();
    }

    private void createTable() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1 BIGINT NOT NULL,
                    uid2 BIGINT NOT NULL,
                    name VARCHAR(64) NOT NULL UNIQUE,
                    time BIGINT NOT NULL,
                    enable TINYINT NOT NULL
                )""".formatted(TABLE_NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, time, enable) values (?, ?, ?, ?, ?)".formatted(TABLE_NAME));
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementDelete() throws SQLException {
        if (this.statementDelete == null) {
            this.statementDelete = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE uid1=? AND uid2=? LIMIT 1".formatted(TABLE_NAME));
        }
        return statementDelete;
    }

    private @NotNull PreparedStatement getStatementUpdate() throws SQLException {
        if (this.statementUpdate == null) {
            this.statementUpdate = this.connection.prepareStatement
                    ("UPDATE %s SET name=?, time=?, enable=? WHERE uid1=? AND uid2=? LIMIT 1".formatted(TABLE_NAME));
        }
        return statementUpdate;
    }

    private PreparedStatement getStatementQueryByName() throws SQLException {
        if (this.statementQueryByName == null) {
            this.statementQueryByName = this.connection.prepareStatement
                    ("SELECT uid1, uid2, name, time, enable FROM %s WHERE name=? LIMIT 1".formatted(TABLE_NAME));
        }
        return statementQueryByName;
    }

    private @NotNull PreparedStatement getStatementQueryByUuid() throws SQLException {
        if (this.statementQueryByUuid == null) {
            this.statementQueryByUuid = this.connection.prepareStatement
                    ("SELECT uid1, uid2, name, time, enable FROM %s WHERE uid1=? AND uid2=? LIMIT 1".formatted(TABLE_NAME));
        }
        return this.statementQueryByUuid;
    }

    private @NotNull NameInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final long uid1 = resultSet.getLong(1);
        final long uid2 = resultSet.getLong(2);

        final String name = resultSet.getString(3);
        final long time = resultSet.getLong(4);
        final int enable = resultSet.getInt(5);


        return new NameInfo(
                new UUID(uid1, uid2),
                name,
                time,
                enable != 0
        );
    }

    private @Nullable NameInfo parseOne(@NotNull ResultSet resultSet) throws SQLException {
        final NameInfo nameInfo;
        try {
            if (resultSet.next()) nameInfo = this.parseRow(resultSet);
            else nameInfo = null;

            if (resultSet.next()) throw new SQLException("不应该还有数据！");

        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }
        resultSet.close();

        return nameInfo;
    }

    @Nullable NameInfo queryByName(@NotNull String name) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByName();

        ps.setString(1, name);

        final ResultSet resultSet = ps.executeQuery();

        return this.parseOne(resultSet);
    }

    @Nullable NameInfo queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByUuid();

        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());

        final ResultSet resultSet = ps.executeQuery();

        return this.parseOne(resultSet);
    }

    int insert(@NotNull NameInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();

        ps.setLong(1, info.uuid().getMostSignificantBits());
        ps.setLong(2, info.uuid().getLeastSignificantBits());
        ps.setString(3, info.name());
        ps.setLong(4, info.time());
        ps.setInt(5, info.enable() ? 1 : 0);

        return ps.executeUpdate();
    }

    int deleteByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementDelete();

        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());

        return ps.executeUpdate();
    }

    int updateByUuid(@NotNull NameInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementUpdate();

        ps.setString(1, info.name());
        ps.setLong(2, info.time());
        ps.setInt(3, info.enable() ? 1 : 0);

        ps.setLong(4, info.uuid().getMostSignificantBits());
        ps.setLong(5, info.uuid().getLeastSignificantBits());
        return ps.executeUpdate();
    }

}
