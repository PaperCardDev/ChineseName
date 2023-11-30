package cn.paper_card.chinese_name;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

public class MyConnection implements DatabaseApi.MySqlConnection {

    private long lastUse = -1;

    private Connection connection = null;

    @Override
    public long getLastUseTime() {
        return this.lastUse;
    }

    @Override
    public void setLastUseTime() {
        this.lastUse = System.currentTimeMillis();
    }

    @Override
    public @NotNull Connection getRawConnection() throws SQLException {
        if (this.connection != null) return this.connection;
        this.connection = Util.connectMySQL(System.getenv("MYSQL_ADDRESS"), System.getenv("MYSQL_USER"), System.getenv("MYSQL_PASSWORD"));
        return this.connection;
    }

    @Override
    public int getConnectCount() {
        return 0;
    }

    @Override
    public void testConnection() {
    }

    @Override
    public void close() throws SQLException {
        final Connection c = this.connection;
        this.connection = null;
        if (c != null) c.close();
    }

    @Override
    public void handleException(@NotNull SQLException e) throws SQLException {
        this.close();
    }
}
