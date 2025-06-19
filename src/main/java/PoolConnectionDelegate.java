import api.ConnectionMethod;

import java.sql.Connection;

public class PoolConnectionDelegate extends DelegatingConnectionAdapter implements PoolConnection{
    protected final PoolConnection poolConnection;
    public PoolConnectionDelegate(PoolConnection connection) {
        super(connection);
        this.poolConnection = connection;
    }

    @Override
    public Connection getWrappedConnection() {
        doActionBeforeEachMethod();
        return poolConnection.getWrappedConnection();
    }

    @Override
    public Connection uncheckGetWrappedConnection() {
        doActionBeforeEachMethod(ConnectionMethod.UNCHECKED_GET_WRAPPED_CONNECTION);
        return poolConnection.uncheckGetWrappedConnection();
    }

    @Override
    public void grab() {
        doActionBeforeEachMethod();
        poolConnection.grab();
    }
}
