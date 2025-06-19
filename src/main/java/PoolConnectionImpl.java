import api.EntityFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class PoolConnectionImpl implements PoolConnection {
    private static Logger log = Logger.getLogger(PoolConnection.class.getName());
    private final Connection connection;
    private final EntityFactory<JdbcPool> poolFactory;
    private final boolean autocloseStatements;
    private final List<Statement> statements = new ArrayList<>();


    private boolean errorOccurred;
    private volatile boolean pooled;

    public PoolConnectionImpl(EntityFactory<JdbcPool> poolFactory, Connection connection, boolean autocloseStatements) {
        this.connection = connection;
        this.poolFactory = poolFactory;
        this.autocloseStatements = autocloseStatements;
    }


    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    @Override
    public Connection getWrappedConnection() {
        return this.connection;
    }

    @Override
    public Connection uncheckGetWrappedConnection() {
        return this.connection;
    }

    @Override
    public void grab() {
        pooled = false;
    }

    @Override
    public Statement createStatement() throws SQLException {
        try {
            return addStatement(wrapSqlExceptions(connection.createStatement(),Statement.class));
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        try {
         return addStatement(wrapSqlExceptions(connection.prepareStatement(sql),PreparedStatement.class));
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        try {
         return addStatement(wrapSqlExceptions(connection.prepareCall(sql),CallableStatement.class));
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
       try {
        return connection.nativeSQL(sql);
       }catch (SQLException exception){
           errorOccurred=true;
           throw exception;
       }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        try {
            return connection.getAutoCommit();
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        try {
            connection.setAutoCommit(autoCommit);
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public void commit() throws SQLException {
        try {
            connection.commit();
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public void rollback() throws SQLException {
        try {
            connection.rollback();
        }catch (SQLException exception){
            errorOccurred=true;
            throw exception;
        }
    }

    @Override
    public void close() throws SQLException {
        if(pooled){
            log.warning("an attempt to release already pooled connection");
            return;
        }
        try {
            if(!connection.getAutoCommit()){
                log.warning("Returning connection with auto commit set to false");
                Exception stackTrace = new Exception("Stack trace");
                log.warning(stackTrace.toString());
                errorOccurred = true;
            }
        }catch (SQLException ex){
            log.warning(String.format("Error when returning connection to the pool '%s'",ex));
            errorOccurred = true;
        }
        if(autocloseStatements){
            int failedCount = 0;
            SQLException exception = null;
            for (Statement statement : statements){
                try {
                    statement.close();
                }catch (SQLException e){
                    failedCount++;
                    exception = e;
                }
            }
            if(exception != null){
                log.warning(exception.getMessage());
                log.warning(String.format("error when auto closing %d statement(s)",failedCount));
                errorOccurred = true;
            }
            statements.clear();
            boolean check = errorOccurred;
            JdbcPool pool = poolFactory.create();
            if(pool == null){
                String msg = String.format("can't create jdbc connection because jdbc pool is undefined/null closing underlying connection '%s' '%s' ", this, connection);
                log.warning(msg);
                connection.close();
            }else {
                errorOccurred = false;
                pooled = true;
                pool.release(this,check);
            }
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try {
            return connection.isClosed();
        }catch (SQLException e){
            errorOccurred= true;
            throw e;
        }
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        try {
            return wrapSqlExceptions(connection.getMetaData(), DatabaseMetaData.class);
        }catch (SQLException e ){
            errorOccurred = true;
            throw e;
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        try {
            return connection.isReadOnly();
        }catch (SQLException e ){
            errorOccurred = true;
            throw e;
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        try {
            connection.setReadOnly(readOnly);
        }catch (Exception e){
            errorOccurred =true;
            throw e;
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        return "";
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {

    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return Map.of();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {

    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return "";
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public String getSchema() throws SQLException {
        return "";
    }

    @Override
    public void setSchema(String schema) throws SQLException {

    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public <T> T unwarp(Class<T> iface) throws SQLException {
        try {
            if (iface.isInstance(this)) {
                return (T) this;
            }
            return connection.unwrap(iface);
        } catch (SQLException e) {
            errorOccurred = true;
            throw e;
        }
    }

    private <T> T wrapSqlExceptions(final T object, Class<T> cls) {
        return cls.cast(Proxy.newProxyInstance(PoolConnectionImpl.class.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                try {
                    // actually call the real object’s method
                    return method.invoke(object, args);
                } catch (InvocationTargetException e) {
                    // reflection wraps any exception thrown by the target in ITE
                    Throwable target = e.getTargetException();
                    if (target instanceof SQLException)
                        errorOccurred = true;      // mark that we saw a SQL error
                    // re-throw the real exception (not the wrapper)
                    throw (target != null ? target : e);
                }
            }
        }));
    }


    private <T extends Statement> T addStatement(T statement) {
        if (autocloseStatements)
            statements.add(statement);
        return statement;

    }
}
