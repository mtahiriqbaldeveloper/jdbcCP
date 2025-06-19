package api;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

// Enhanced EntityPool with monitoring capabilities
public class EntityPoolMonitor<T> {
    private static final Logger log = Logger.getLogger(EntityPoolMonitor.class.getName());

    private final AtomicLong totalConnectionsCreated = new AtomicLong(0);
    private final AtomicLong totalConnectionsReused = new AtomicLong(0);
    private final AtomicLong totalConnectionsReleased = new AtomicLong(0);
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicLong totalGetRequests = new AtomicLong(0);

    // Add these methods to your EntityPool class

    public void recordConnectionCreated() {
        totalConnectionsCreated.incrementAndGet();
        logPoolStats();
    }

    public void recordConnectionReused() {
        totalConnectionsReused.incrementAndGet();
    }

    public void recordConnectionReleased() {
        totalConnectionsReleased.incrementAndGet();
    }

    public void recordGetRequest(long waitTimeMs) {
        totalGetRequests.incrementAndGet();
        totalWaitTime.addAndGet(waitTimeMs);
    }

    public void logPoolStats() {
        log.info(String.format(
                "Pool Stats - Created: %d, Reused: %d, Released: %d, Avg Wait: %.2fms, Reuse Rate: %.2f%%",
                totalConnectionsCreated.get(),
                totalConnectionsReused.get(),
                totalConnectionsReleased.get(),
                getAverageWaitTime(),
                getReuseRate()
        ));
    }

    public double getAverageWaitTime() {
        long requests = totalGetRequests.get();
        return requests > 0 ? (double) totalWaitTime.get() / requests : 0.0;
    }

    public double getReuseRate() {
        long total = totalConnectionsCreated.get() + totalConnectionsReused.get();
        return total > 0 ? (double) totalConnectionsReused.get() / total * 100 : 0.0;
    }

    public PoolStatistics getStatistics() {
        return new PoolStatistics(
                totalConnectionsCreated.get(),
                totalConnectionsReused.get(),
                totalConnectionsReleased.get(),
                getAverageWaitTime(),
                getReuseRate()
        );
    }

    public static class PoolStatistics {
        public final long connectionsCreated;
        public final long connectionsReused;
        public final long connectionsReleased;
        public final double averageWaitTime;
        public final double reuseRate;

        public PoolStatistics(long created, long reused, long released,
                              double avgWait, double reuseRate) {
            this.connectionsCreated = created;
            this.connectionsReused = reused;
            this.connectionsReleased = released;
            this.averageWaitTime = avgWait;
            this.reuseRate = reuseRate;
        }

        @Override
        public String toString() {
            return String.format(
                    "PoolStats{created=%d, reused=%d, released=%d, avgWait=%.2fms, reuseRate=%.1f%%}",
                    connectionsCreated, connectionsReused, connectionsReleased,
                    averageWaitTime, reuseRate
            );
        }
    }
}