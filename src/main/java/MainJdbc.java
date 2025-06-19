import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class MainJdbc {
    private final static Logger log = Logger.getLogger(MainJdbc.class.getName());
    private final ThreadGroup jdbcThreadGroup = new ThreadGroup("jdbc-thread-group");

    public static void main(String[] args) throws IllegalAccessException, SQLException {
        Properties properties = new Properties();
        String driverClass = "org.postgresql.Driver";
        String url = "jdbc:postgresql://localhost:5433/test";
        properties.setProperty("user", "admin");
        properties.setProperty("password", "admin");

        MainJdbc mainJdbc = new MainJdbc();

        JdbcPoolBuilder jdbcPoolBuilder = new JdbcPoolBuilder(properties);
        jdbcPoolBuilder.setThreadGroup(mainJdbc.jdbcThreadGroup);
        jdbcPoolBuilder.setDriverClass(driverClass);
        jdbcPoolBuilder.setUrl(url);
        jdbcPoolBuilder.setDefaultCheckoutTime(2000);
        JdbcPool build = jdbcPoolBuilder.build();
        DataSource dataSource = build;

        if (build != null) {

            for (int i = 0; i < 5; i++) {
                int poolSize = build.getPoolSize();
                log.info("this is the pool size :" + poolSize);
                Connection connection = dataSource.getConnection();
                mainJdbc.executeStatement(connection);
                System.out.println("Connection used: " + connection.hashCode());
            }
        }
    }

    public void executeStatement(Connection connection) throws SQLException {
        // 3. Insert data
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO myschema.users (username, email) VALUES (?, ?)")) {
            insert.setString(1, "moin");
            insert.setString(2, "moin@example.com");
            int rows = insert.executeUpdate();
            System.out.println("Inserted rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
