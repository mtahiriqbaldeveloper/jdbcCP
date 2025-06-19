import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class SimplePoolTest {

    public static void main(String[] args) {
        // Create your pool
        Properties properties = new Properties();
        String driverClass = "org.postgresql.Driver";
        String url = "jdbc:postgresql://localhost:5433/test";
        properties.setProperty("user", "admin");
        properties.setProperty("password", "admin");

        JdbcPoolBuilder builder = new JdbcPoolBuilder(properties);
        builder.setUrl(url);
        builder.setDriverClass(driverClass);
        builder.setAutocloseStatements(true);
        builder.getEntityPoolBuilder()
                .initialSize(1)
                .maxSize(1)
                .defaultCheckoutTime(5000);

        try {
            JdbcPool pool = builder.build();

//            testConnectionReuse(pool);
//           ConnectionPoolTester.runAllTests(pool, 10);
            testUnderlyingConnectionReuse(pool);
            pool.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String getConnectionIdentity(Connection conn) {
        if (conn instanceof PoolConnection) {
            // If it's a pool connection, get the underlying connection's identity
            return conn.toString();
        }
        return conn.toString();
    }
//    public static void testConnectionReuse(JdbcPool pool) {
//        System.out.println("\n=== Connection Reuse Test ===");
//        try {
//            // Get connection and note its identity
//            Connection conn1 = pool.getConnection();
//            String conn1Identity = getConnectionIdentity(conn1);
//            System.out.println("First connection: " + conn1Identity);
//            conn1.close();
//
//            // Get another connection - should be the same one reused
//            Connection conn2 = pool.getConnection();
//            String conn2Identity = getConnectionIdentity(conn2);
//            System.out.println("Second connection: " + conn2Identity);
//
//            if (conn1Identity.equals(conn2Identity)) {
//                System.out.println("✓ Connection reuse working - same connection returned");
//            } else {
//                System.out.println("✗ Connection reuse NOT working - different connection returned");
//            }
//
//            conn2.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
    public static void testPoolStatistics(JdbcPool pool) {
        System.out.println("=== Pool Statistics Test ===");
        System.out.println("Initial pool size: " + pool.getPoolSize());

        try {
            // Get a connection
            Connection conn1 = pool.getConnection();
            System.out.println("After getting 1 connection, pool size: " + pool.getPoolSize());

            // Get another connection
            Connection conn2 = pool.getConnection();
            System.out.println("After getting 2 connections, pool size: " + pool.getPoolSize());

            // Release first connection
            conn1.close();
            Thread.sleep(100); // Give time for release
            System.out.println("After releasing 1 connection, pool size: " + pool.getPoolSize());

            // Release second connection
            conn2.close();
            Thread.sleep(100);
            System.out.println("After releasing all connections, pool size: " + pool.getPoolSize());

        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void quickPoolTest(JdbcPool pool) {
        System.out.println("=== Quick Pool Test ===");

        try {
            // Test 1: Check initial state
            System.out.println("Initial pool size: " + pool.getPoolSize());

            // Test 2: Get and release connection
            Connection conn1 = pool.getConnection();
            System.out.println("Got connection, pool size: " + pool.getPoolSize());

            conn1.close();
            Thread.sleep(100); // Allow time for release
            System.out.println("Released connection, pool size: " + pool.getPoolSize());

            // Test 3: Multiple connections
            Connection[] connections = new Connection[3];
            for (int i = 0; i < 3; i++) {
                connections[i] = pool.getConnection();
                System.out.println("Got connection " + (i+1) + ", pool size: " + pool.getPoolSize());
            }

            // Release all
            for (Connection conn : connections) {
                conn.close();
            }
            Thread.sleep(100);
            System.out.println("Released all connections, pool size: " + pool.getPoolSize());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void testConnectionReuse(JdbcPool pool) {
        System.out.println("\n=== Connection Reuse Test ===");

        try {
            // Method 1: Test using underlying connection identity
            testUnderlyingConnectionReuse(pool);

            // Method 2: Test using pool size metrics
            testPoolSizeMetrics(pool);

            // Method 3: Test using connection validation
            testConnectionValidation(pool);

        } catch (SQLException e) {
            System.err.println("Test failed with SQLException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testUnderlyingConnectionReuse(JdbcPool pool) throws SQLException {
        System.out.println("\n--- Testing Underlying Connection Reuse ---");

        // Get first connection and extract underlying connection
        Connection conn1 = pool.getConnection();
        Connection underlying1 = getUnderlyingConnection(conn1);
        String underlying1Identity = System.identityHashCode(underlying1) + "@" + underlying1.getClass().getSimpleName();
        System.out.println("First underlying connection: " + underlying1Identity);
        conn1.close();

        // Small delay to ensure connection is returned to pool
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Get second connection
        Connection conn2 = pool.getConnection();
        Connection underlying2 = getUnderlyingConnection(conn2);
        String underlying2Identity = System.identityHashCode(underlying2) + "@" + underlying2.getClass().getSimpleName();
        System.out.println("Second underlying connection: " + underlying2Identity);

        if (underlying1Identity.equals(underlying2Identity)) {
            System.out.println("✓ Underlying connection reuse working - same connection returned");
        } else {
            System.out.println("✗ Underlying connection reuse NOT working - different connection returned");
        }

        conn2.close();
    }

    private static void testPoolSizeMetrics(JdbcPool pool) {
        System.out.println("\n--- Testing Pool Size Metrics ---");

        int initialSize = pool.getPoolSize();
        System.out.println("Initial pool size: " + initialSize);

        try {
            Connection conn1 = pool.getConnection();
            int sizeAfterGet = pool.getPoolSize();
            System.out.println("Pool size after getting connection: " + sizeAfterGet);

            conn1.close();

            // Small delay for connection return processing
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            int sizeAfterReturn = pool.getPoolSize();
            System.out.println("Pool size after returning connection: " + sizeAfterReturn);

            if (sizeAfterReturn >= sizeAfterGet) {
                System.out.println("✓ Connection appears to be returned to pool (size maintained/increased)");
            } else {
                System.out.println("✗ Connection may not be returned to pool (size decreased)");
            }

        } catch (SQLException e) {
            System.err.println("Pool size test failed: " + e.getMessage());
        }
    }

    private static void testConnectionValidation(JdbcPool pool) {
        System.out.println("\n--- Testing Connection Validation ---");

        try {
            // Get connection, perform operation, close
            Connection conn1 = pool.getConnection();
            boolean valid1 = isConnectionValid(conn1);
            System.out.println("First connection valid: " + valid1);
            conn1.close();

            // Get another connection and test
            Connection conn2 = pool.getConnection();
            boolean valid2 = isConnectionValid(conn2);
            System.out.println("Second connection valid: " + valid2);

            if (valid1 && valid2) {
                System.out.println("✓ Both connections are valid - pool is working correctly");
            } else {
                System.out.println("✗ Connection validation failed - pool may have issues");
            }

            conn2.close();

        } catch (SQLException e) {
            System.err.println("Connection validation test failed: " + e.getMessage());
        }
    }

    private static Connection getUnderlyingConnection(Connection conn) {
        // For PoolConnectionImpl, use the getWrappedConnection method
        if (conn instanceof PoolConnection) {
            return ((PoolConnection) conn).getWrappedConnection();
        }

        // Fallback: try unwrap method
        try {
            return conn.unwrap(Connection.class);
        } catch (SQLException e) {
            System.err.println("Could not unwrap connection: " + e.getMessage());
            return conn; // Return the connection itself as fallback
        }
    }

    private static boolean isConnectionValid(Connection conn) {
        try {
            // Test with a simple query or validation
            return conn.isValid(1); // 1 second timeout
        } catch (SQLException e) {
            try {
                // Fallback: try a simple statement
                conn.createStatement().close();
                return true;
            } catch (SQLException e2) {
                return false;
            }
        }
    }

    // Enhanced version that also tests concurrent access
    public static void testConnectionReuseWithConcurrency(JdbcPool pool) {
        System.out.println("\n=== Concurrent Connection Reuse Test ===");

        final int threadCount = 5;
        final int operationsPerThread = 10;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        Connection conn = pool.getConnection();
                        // Simulate some work
                        Thread.sleep(1);
                        conn.close();
                        System.out.printf("Thread %d completed operation %d%n", threadId, j + 1);
                    } catch (Exception e) {
                        System.err.printf("Thread %d failed on operation %d: %s%n", threadId, j + 1, e.getMessage());
                    }
                }
            });
        }

        // Start all threads
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("Concurrent test completed in %d ms%n", endTime - startTime);
        System.out.println("Final pool size: " + pool.getPoolSize());
    }
}