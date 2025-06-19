import java.sql.Connection;

public interface PoolConnection extends Connection {
    Connection getWrappedConnection();
    Connection uncheckGetWrappedConnection();
    void grab();
}
