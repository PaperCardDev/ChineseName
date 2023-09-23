package cn.paper_card.chinese_name;

import cn.paper_card.database.DatabaseConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class ChineseNameTable {

    private static final String TABLE_NAME = "chinese_name";

    private final PreparedStatement statementQueryByUuid;
    private final PreparedStatement statementQueryByName;

    private final PreparedStatement statementInsert;

    private final PreparedStatement statementUpdate;

    ChineseNameTable(@NotNull Connection connection) throws SQLException {
        this.createTable(connection);

        try {
            this.statementQueryByUuid = connection.prepareStatement
                    ("SELECT uid1, uid2, name, time, in_use FROM %s WHERE uid1=? AND uid2=?".formatted(TABLE_NAME));

            this.statementQueryByName = connection.prepareStatement
                    ("SELECT uid1, uid2, name, time, in_use FROM %s WHERE name=?".formatted(TABLE_NAME));

            this.statementInsert = connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, time, in_use) values (?, ?, ?, ?, ?)".formatted(TABLE_NAME));

            this.statementUpdate = connection.prepareStatement
                    ("UPDATE %s SET name=?, time=?, in_use=? WHERE uid1=? AND uid2=?".formatted(TABLE_NAME));

        } catch (SQLException e) {
            try {
                this.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }
    }

    private void createTable(@NotNull Connection connection) throws SQLException {
        DatabaseConnection.createTable(connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1 INTEGER NOT NULL,
                    uid2 INTEGER NOT NULL,
                    name VARCHAR(24) NOT NULL,
                    time INTEGER NOT NULL,
                    in_use INTEGER NOT NULL
                )""".formatted(TABLE_NAME));
    }

    void close() throws SQLException {
        DatabaseConnection.closeAllStatements(this.getClass(), this);
    }

    @NotNull List<ChineseNameApi.NameInfo> parse(@NotNull ResultSet resultSet) throws SQLException {

        final LinkedList<ChineseNameApi.NameInfo> list = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final long uid1 = resultSet.getLong(1);
                final long uid2 = resultSet.getLong(2);
                final String name = resultSet.getString(3);
                final long time = resultSet.getLong(4);
                final int inUse = resultSet.getInt(5);
                list.add(new ChineseNameApi.NameInfo(
                        new UUID(uid1, uid2),
                        name,
                        time,
                        inUse != 0
                ));
            }
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

    @NotNull List<ChineseNameApi.NameInfo> queryByName(@NotNull String name) throws SQLException {
        final PreparedStatement ps = this.statementQueryByName;
        ps.setString(1, name);
        final ResultSet resultSet = ps.executeQuery();
        return this.parse(resultSet);
    }

    @NotNull List<ChineseNameApi.NameInfo> queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.statementQueryByUuid;
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());

        final ResultSet resultSet = ps.executeQuery();
        return this.parse(resultSet);
    }

    int insert(@NotNull ChineseNameApi.NameInfo info) throws SQLException {
        final PreparedStatement ps = this.statementInsert;
        ps.setLong(1, info.uuid().getMostSignificantBits());
        ps.setLong(2, info.uuid().getLeastSignificantBits());
        ps.setString(3, info.name());
        ps.setLong(4, info.time());
        ps.setInt(5, info.inUse() ? 1 : 0);
        return ps.executeUpdate();
    }

    int updateByUuid(@NotNull ChineseNameApi.NameInfo info) throws SQLException {
        final PreparedStatement ps = this.statementUpdate;
        ps.setString(1, info.name());
        ps.setLong(2, info.time());
        ps.setInt(3, info.inUse() ? 1 : 0);
        ps.setLong(4, info.uuid().getMostSignificantBits());
        ps.setLong(5, info.uuid().getLeastSignificantBits());
        return ps.executeUpdate();
    }
}
