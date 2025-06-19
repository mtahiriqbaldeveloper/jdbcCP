import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class JdbcPool implements DataSource {

    private final EntityPool<PoolConnection> pool;
    private final Collection<ApplicationStatusProvider> statusProviders = new CopyOnWriteArrayList<>();
    private final StatementCacheConnectionFactory statementCacheConnectionFactory;

    public JdbcPool(EntityPool<PoolConnection> pool)throws IllegalArgumentException {
        this(pool,null);
    }

    public JdbcPool(EntityPool<PoolConnection> pool, StatementCacheConnectionFactory statementCacheConnectionFactory)throws IllegalArgumentException {
        if(pool == null){
            throw new IllegalArgumentException("can't create jdbc pool with null entity pool");
        }
        this.pool = pool;
        this.statementCacheConnectionFactory = statementCacheConnectionFactory;
    }
    public void close(){
        pool.close();
    }
    public int getPoolSize(){
        if(pool != null){
            return pool.getCurrentSize();
        }else {
            return 0;
        }
    }
    @Override
    public Connection getConnection() throws SQLException {
        try {
            PoolConnection con = pool.getEntity();
            if(con == null){
                throw new SQLException("No connection are available in the connection pool");
            }
            con.grab();
            return con;
        }catch (InterruptedException| IllegalStateException e){
            throw new SQLException("can't retrieve jdbc connection. Reason: "+ e);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException(String.format("Unspported call to datasource.getconnection(username, password) %s",username));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException("unsupported call to DataSource.setLogwriter(PrintWriter)");
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("unSupported call to datasource.getparentLogger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public void setApplicationStatusProviders(Collection<ApplicationStatusProvider> statusProviders){
        this.statusProviders.clear();
        this.statusProviders.addAll(statusProviders);
    }

    public StatementCacheConnectionFactory getStatementCacheConnectionFactory() {
        return statementCacheConnectionFactory;
    }

    public void release(PoolConnection connection, boolean check){
        for(ApplicationStatusProvider statusProvider : statusProviders)
            check |=statusProvider.errorOccurred();
        pool.release(connection, check);

    }
}
