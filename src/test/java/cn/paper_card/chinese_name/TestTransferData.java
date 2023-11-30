package cn.paper_card.chinese_name;

import cn.paper_card.chinese_name.api.NameInfo;
import cn.paper_card.chinese_name.api.exception.NameRegisteredException;
import cn.paper_card.database.api.Util;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TestTransferData {
    @Test
    @Ignore
    public void test1() throws SQLException {
        final Connection connection = Util.connectSQLite(new File("C:\\Users\\Administrator\\Desktop\\Database\\Important.db"));

        final PreparedStatement ps = connection.prepareStatement("SELECT * FROM chinese_name");

        final MyConnection myConnection = new MyConnection();

        final NameServiceImpl service = new NameServiceImpl(myConnection);

        final ResultSet resultSet = ps.executeQuery();

        while (resultSet.next()) {
            final long uid1 = resultSet.getLong(1);
            final long uid2 = resultSet.getLong(2);
            final String name = resultSet.getString(3);
            final long time = resultSet.getLong(4);
            final int inUse = resultSet.getInt(5);

            final NameInfo nameInfo = new NameInfo(new UUID(uid1, uid2), name, time, inUse != 0);

            System.out.println(nameInfo);

            try {
                service.addOrUpdateByUuid(nameInfo);
            } catch (NameRegisteredException e) {
                final String string = e.toString();
                System.out.println(string);
            }
        }


        resultSet.close();

        connection.close();
        myConnection.close();
    }
}
