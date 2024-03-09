package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.ApplicationInfo;
import cn.paper_card.database.api.Parser;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.List;
import java.util.UUID;

class ApplicationTable extends Parser<ApplicationInfo> {

    private static final String NAME = "chinese_name_app";

    private PreparedStatement statementQueryById = null;

    private PreparedStatement statementQueryByName = null;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementDeleteById = null;

    private PreparedStatement statementQueryWithPage = null;

    private PreparedStatement psQueryByUuid = null;

    private PreparedStatement psQueryCount = null;

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
                    time    BIGINT NOT NULL,
                    coins   BIGINT NOT NULL
                );""".formatted(NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, name, time, coins) VALUES (?, ?, ?, ?, ?)".formatted(NAME),
                            Statement.RETURN_GENERATED_KEYS);
        }
        return this.statementInsert;
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
                    ("SELECT id, uid1, uid2, name, time, coins FROM %s WHERE id=? LIMIT 1".formatted(NAME));
        }
        return this.statementQueryById;
    }

    private @NotNull PreparedStatement getStatementQueryByName() throws SQLException {
        if (this.statementQueryByName == null) {
            this.statementQueryByName = this.connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time, coins FROM %s WHERE name=? LIMIT 1".formatted(NAME));
        }
        return this.statementQueryByName;
    }

    private @NotNull PreparedStatement getStatementQueryWithPage() throws SQLException {

        if (this.statementQueryWithPage == null) {
            this.statementQueryWithPage = this.connection.prepareStatement
                    ("SELECT id, uid1, uid2, name, time, coins FROM %s LIMIT ? OFFSET ?".formatted(NAME));
        }

        return this.statementQueryWithPage;
    }

    private @NotNull PreparedStatement getPsQueryByUuid() throws SQLException {
        if (this.psQueryByUuid == null) {
            this.psQueryByUuid = this.connection.prepareStatement("""
                    SELECT id, uid1, uid2, name, time, coins
                    FROM %s
                    WHERE (uid1, uid2) = (?, ?)
                    LIMIT 1;""".formatted(NAME));
        }
        return this.psQueryByUuid;
    }

    private @NotNull PreparedStatement getPsQueryCount() throws SQLException {
        if (this.psQueryCount == null) {
            this.psQueryCount = this.connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM %s;""".formatted(NAME));
        }
        return this.psQueryCount;
    }

    int queryCount() throws SQLException {
        final PreparedStatement c = this.getPsQueryCount();
        final ResultSet resultSet = c.executeQuery();
        return Parser.parseOneInt(resultSet);
    }

    @Override
    public @NotNull ApplicationInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final int id = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final String name = resultSet.getString(4);
        final long time = resultSet.getLong(5);
        final long coins = resultSet.getLong(6);
        return new ApplicationInfo(
                id,
                new UUID(uid1, uid2),
                name,
                coins,
                time
        );
    }

    int insert(@NotNull ApplicationInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setLong(1, info.uuid().getMostSignificantBits());
        ps.setLong(2, info.uuid().getLeastSignificantBits());
        ps.setString(3, info.name());
        ps.setLong(4, info.time());
        ps.setLong(5, info.coins());

        ps.executeUpdate();

        final ResultSet generatedKeys = ps.getGeneratedKeys();

        return Parser.parseOneInt(generatedKeys);
    }

    int deleteById(int id) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteById();

        ps.setInt(1, id);

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

    @Nullable ApplicationInfo queryByUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getPsQueryByUuid();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
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
