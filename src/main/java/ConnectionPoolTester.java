import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ConnectionPoolTester {
    private static final Logger log = Logger.getLogger(ConnectionPoolTester.class.getName());

    // Test 1: Basic Pool Statistics Test
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

    // Test 2: Connection Reuse Test
    public static void testConnectionReuse(JdbcPool pool) {
        System.out.println("\n=== Connection Reuse Test ===");
        try {
            // Get connection and note its identity
            Connection conn1 = pool.getConnection();
            String conn1Identity = getConnectionIdentity(conn1);
            System.out.println("First connection: " + conn1Identity);
            conn1.close();

            // Get another connection - should be the same one reused
            Connection conn2 = pool.getConnection();
            String conn2Identity = getConnectionIdentity(conn2);
            System.out.println("Second connection: " + conn2Identity);

            if (conn1Identity.equals(conn2Identity)) {
                System.out.println("✓ Connection reuse working - same connection returned");
            } else {
                System.out.println("✗ Connection reuse NOT working - different connection returned");
            }

            conn2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Test 3: Concurrent Access Test
    public static void testConcurrentAccess(JdbcPool pool, int threadCount, int operationsPerThread) {
        System.out.println("\n=== Concurrent Access Test ===");
        System.out.println("Testing with " + threadCount + " threads, " + operationsPerThread + " operations each");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int j = 0; j < operationsPerThread; j++) {
                        Connection conn = pool.getConnection();

                        // Simulate some work
                        performDummyQuery(conn, threadId, j);

                        conn.close();
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        try {
            completeLatch.await(30, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            System.out.println("Concurrent test completed in " + (endTime - startTime) + "ms");
            System.out.println("Final pool size: " + pool.getPoolSize());

        } catch (InterruptedException e) {
            System.err.println("Concurrent test interrupted");
        }

        executor.shutdown();
    }

    // Test 4: Pool Exhaustion Test
    public static void testPoolExhaustion(JdbcPool pool, int maxPoolSize) {
        System.out.println("\n=== Pool Exhaustion Test ===");
        Connection[] connections = new Connection[maxPoolSize + 1];

        try {
            // Try to get maxPoolSize connections
            for (int i = 0; i < maxPoolSize; i++) {
                connections[i] = pool.getConnection();
                System.out.println("Got connection " + (i + 1) + "/" + maxPoolSize);
            }

            System.out.println("Pool size after getting all connections: " + pool.getPoolSize());

            // Try to get one more - this should either timeout or fail
            long startTime = System.currentTimeMillis();
            try {
                connections[maxPoolSize] = pool.getConnection();
                long waitTime = System.currentTimeMillis() - startTime;
                System.out.println("Got extra connection after " + waitTime + "ms - this might indicate pool expansion");
            } catch (SQLException e) {
                long waitTime = System.currentTimeMillis() - startTime;
                System.out.println("✓ Pool exhaustion working - failed to get extra connection after " + waitTime + "ms");
            }

        } catch (SQLException e) {
            System.err.println("Pool exhaustion test failed: " + e.getMessage());
        } finally {
            // Release all connections
            for (Connection conn : connections) {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    // Test 5: Connection Validation Test
    public static void testConnectionValidation(JdbcPool pool) {
        System.out.println("\n=== Connection Validation Test ===");
        try {
            Connection conn = pool.getConnection();

            // Test if connection is valid
            boolean isValid = conn.isValid(5);
            System.out.println("Connection validity check: " + (isValid ? "✓ Valid" : "✗ Invalid"));

            // Test basic operation
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("✓ Connection can execute queries");
                }
            }

            conn.close();
        } catch (SQLException e) {
            System.err.println("Connection validation failed: " + e.getMessage());
        }
    }

    // Helper method to get connection identity
    private static String getConnectionIdentity(Connection conn) {
        if (conn instanceof PoolConnection) {
            // If it's a pool connection, get the underlying connection's identity
            return conn.toString();
        }
        return conn.toString();
    }

    // Helper method to perform dummy query
    private static void performDummyQuery(Connection conn, int threadId, int operation) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT ? as thread_id, ? as operation")) {
            stmt.setInt(1, threadId);
            stmt.setInt(2, operation);
            ResultSet rs = stmt.executeQuery();
            rs.next(); // Just consume the result
        } catch (SQLException e) {
            System.err.println("Query failed for thread " + threadId + ", operation " + operation + ": " + e.getMessage());
        }
    }

    // Main test runner
    public static void runAllTests(JdbcPool pool, int maxPoolSize) {
        System.out.println("Starting Connection Pool Tests...");
        System.out.println("=".repeat(50));

        testPoolStatistics(pool);
        testConnectionReuse(pool);
        testConcurrentAccess(pool, 5, 10);
        testPoolExhaustion(pool, maxPoolSize);
        testConnectionValidation(pool);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("All tests completed!");
    }
}