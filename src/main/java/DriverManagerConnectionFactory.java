
import api.EntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class DriverManagerConnectionFactory implements EntityFactory<Connection> {
    Logger log = LoggerFactory.getLogger(DriverManagerConnectionFactory.class.getName());
    public static final String USER_KEY = "ROOT";
    public static final String PASSWORD_KEY = "";
    private final Properties properties;
    private final ConnectionRetriever retriever;
    private final String driverClass;
    private final String login;
    private final String url;

//    public DriverManagerConnectionFactory(String driverClass, String jdbcUrl) throws IllegalAccessException {
//        this.driverClass = driverClass;
//        this.url = jdbcUrl;
//        this.login = null;
//        properties = null;
//        this.retriever = () -> DriverManager.getConnection(this.url);
//    }

    public DriverManagerConnectionFactory(String driverClass, String url, Properties jdbcProperties) {
//        checkDriverClass(driverClass);
        this.driverClass = driverClass;
        this.url = url;
        this.login = jdbcProperties.getProperty("user");
        this.properties = jdbcProperties;
        this.retriever = ()->DriverManager.getConnection(url,properties);
    }

    private static void checkDriverClass(String driverClass) {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Jdbc driver class %s not found", driverClass), e);
        }
    }

    @Override
    public Connection create() {
        try {
            Connection connection = this.retriever.retrieve();
            if (connection == null) {
                throw new IllegalStateException("jdbc unable create the connection");
            } else {
                log.info("jdbc connection was successful");
                return connection;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }

    }

    public Properties getProperties() {
        return properties;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getLogin() {
        return login;
    }

    public String getUrl() {
        return url;
    }

    private interface ConnectionRetriever {
        Connection retrieve() throws SQLException;
    }
}
