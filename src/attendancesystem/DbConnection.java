package attendancesystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConnection {

    private static final String PROPERTIES_FILE = "global.properties";

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            throw new SQLException("Failed to load database properties from " + PROPERTIES_FILE, e);
        }

        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port");
        String name = props.getProperty("db.name");
        String driver = props.getProperty("db.driver");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        if (host == null || port == null || name == null || user == null) {
            throw new SQLException("Incomplete database configuration in " + PROPERTIES_FILE);
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name;

        if (driver != null) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new SQLException("JDBC Driver not found: " + driver, e);
            }
        }

        return DriverManager.getConnection(url, user, password);
    }
}