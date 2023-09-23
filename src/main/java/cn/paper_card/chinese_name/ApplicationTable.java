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

class ApplicationTable {

    private static final String TABLE_NAME = "chinese_name_application";

    private final PreparedStatement statementQueryById;

    private final PreparedStatement statementQueryByName;

    private final PreparedStatement statementInsert;

    private final PreparedStatement statementUpdateByUuid;

    private final PreparedStatement statementDeleteById;

    private final PreparedStatement statementQueryWithLimit;

    ApplicationTable(@NotNull Connection connection) throws SQLException {
        this.createTable(connection);

        try {
            this.statementQueryById = connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s WHERE id=?".formatted(TABLE_NAME));

            this.statementQueryByName = connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s WHERE name=?".formatted(TABLE_NAME));

            this.statementQueryWithLimit = connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time FROM %s LIMIT ?".formatted(TABLE_NAME));


            this.statementInsert = connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, time) VALUES (?, ?, ?, ?)".formatted(TABLE_NAME));

            this.statementUpdateByUuid = connection.prepareStatement
                    ("UPDATE %s SET name=?,time=? WHERE uid1=? AND uid2=?".formatted(TABLE_NAME));

            this.statementDeleteById = connection.prepareStatement
                    ("DELETE FROM %s WHERE id=?".formatted(TABLE_NAME));


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
                    id  INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    uid1    INTEGER NOT NULL,
                    uid2    INTEGER NOT NULL,
                    name    VARCHAR(24) NOT NULL,
                    time    INTEGER NOT NULL
                )""".formatted(TABLE_NAME));
    }

    void close() throws SQLException {
        DatabaseConnection.closeAllStatements(this.getClass(), this);
    }

    private @NotNull List<Integer> parseIds(@NotNull ResultSet resultSet) throws SQLException {

        final LinkedList<Integer> list = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final int id = resultSet.getInt(1);
                list.add(id);
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

    @NotNull List<ChineseNameApi.ApplicationInfo> parse(@NotNull ResultSet resultSet) throws SQLException {

        final LinkedList<ChineseNameApi.ApplicationInfo> list = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final int id = resultSet.getInt(1);
                final long uid1 = resultSet.getLong(2);
                final long uid2 = resultSet.getLong(3);
                final String name = resultSet.getString(4);
                final long time = resultSet.getLong(5);
                list.add(new ChineseNameApi.ApplicationInfo(
                        id,
                        new UUID(uid1, uid2),
                        name,
                        time
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

    @NotNull List<ChineseNameApi.ApplicationInfo> queryById(int id) throws SQLException {
        final PreparedStatement ps = this.statementQueryById;
        ps.setInt(1, id);
        final ResultSet resultSet = ps.executeQuery();
        return this.parse(resultSet);
    }

    @NotNull List<ChineseNameApi.ApplicationInfo> queryByName(@NotNull String name) throws SQLException {
        final PreparedStatement ps = this.statementQueryByName;
        ps.setString(1, name);
        final ResultSet resultSet = ps.executeQuery();
        return this.parse(resultSet);
    }

    @NotNull List<ChineseNameApi.ApplicationInfo> queryWithLimit(int limit) throws SQLException {
        final PreparedStatement ps = this.statementQueryWithLimit;
        ps.setInt(1, limit);
        final ResultSet resultSet = ps.executeQuery();
        return this.parse(resultSet);
    }

    @NotNull List<Integer> insert(@NotNull UUID uuid, @NotNull String name, long time) throws SQLException {
        final PreparedStatement ps = this.statementInsert;
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setString(3, name);
        ps.setLong(4, time);

        ps.executeUpdate();

        final ResultSet generatedKeys = ps.getGeneratedKeys();

        return this.parseIds(generatedKeys);
    }

    int updateByUuid(@NotNull UUID uuid, @NotNull String name, long time) throws SQLException {
        final PreparedStatement ps = this.statementUpdateByUuid;
        ps.setString(1, name);
        ps.setLong(2, time);
        ps.setLong(3, uuid.getMostSignificantBits());
        ps.setLong(4, uuid.getLeastSignificantBits());
        return ps.executeUpdate();
    }

    int deleteById(int id) throws SQLException {
        final PreparedStatement ps = this.statementDeleteById;
        ps.setLong(1, id);
        return ps.executeUpdate();
    }

}
