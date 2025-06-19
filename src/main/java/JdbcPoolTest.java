import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.logging.Logger;

public class JdbcPoolTest {
    private final static Logger log = Logger.getLogger(MainJdbc.class.getName());
    private final ThreadGroup jdbcThreadGroup = new ThreadGroup("jdbc-thread-group");
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("user", "admin");
        props.setProperty("password", "admin");

        JdbcPoolBuilder builder = new JdbcPoolBuilder(props);
        builder.setUrl("jdbc:postgresql://localhost:5433/test");
        builder.setDriverClass("org.postgresql.Driver"); // or your driver
        builder.setDefaultCheckoutTime(2000);
        JdbcPoolTest jdbcPoolTest = new JdbcPoolTest();
        builder.setThreadGroup(jdbcPoolTest.jdbcThreadGroup);
        JdbcPool pool = builder.build();

        createSchemaAndTable(pool);

        int threadCount = 5;
        Runnable task = () -> {
            try (Connection conn = pool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO myschema.users (username, email) VALUES (?, ?)")) {

                String threadName = Thread.currentThread().getName();
                stmt.setString(1, threadName);
                stmt.setString(2, threadName + "@test.com");
                stmt.executeUpdate();

                System.out.println("[" + threadName + "] Connection used: " + conn.hashCode());

                Thread.sleep(300); // simulate delay
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(task, "Thread-" + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("All inserts completed.");
    }

    private static void createSchemaAndTable(JdbcPool pool) throws Exception {
        try (Connection conn = pool.getConnection()) {
            try (PreparedStatement createSchema = conn.prepareStatement(
                    "CREATE SCHEMA IF NOT EXISTS myschema")) {
                createSchema.execute();
            }
            try (PreparedStatement createTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS myschema.users (" +
                            "id SERIAL PRIMARY KEY, " +
                            "username TEXT, " +
                            "email TEXT)")) {
                createTable.execute();
            }
        }
    }
}


