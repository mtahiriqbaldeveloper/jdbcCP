import java.sql.Connection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;

public class StatementCacheConnection extends DelegatingConnectionAdapter {
    private final ConcurrentHashMap<Object, BlockingDeque<CachedPrepareStatement>> preparedStatements = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, BlockingDeque<CachedCallableStatement>> calleStatements = new ConcurrentHashMap<>();
    private final StatementCacheConnectionFactory factory;
    public StatementCacheConnection(StatementCacheConnectionFactory factory,Connection connection) {
        super(connection);
        this.factory = factory;
    }

    public void dropRecentlyNotUsedFromCache(long dropStatementNotUsedSince) {
    }

    public int getCachedStatementsCount() {
        return 0;
    }
}
