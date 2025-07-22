import api.ReleasableStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CachedPrepareStatement extends DelegatingPreparedStatementAdapter implements ReleasableStatement {

    private final StatementCacheConnection connection;
    private final Object key;
    private long lastTimeUsed;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public CachedPrepareStatement(PreparedStatement statement, StatementCacheConnection connection, Object key) {
        super(statement);
        this.connection = connection;
        this.key = key;
    }

    @Override
    public void release() throws SQLException {
        getWrappedStatement().close();
    }
    @Override
    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

    public void setLastTimeUsed(long lastTimeUsed) {
        this.lastTimeUsed = lastTimeUsed;
    }
    public void setOpened(){
        closed.set(false);
    }
}
