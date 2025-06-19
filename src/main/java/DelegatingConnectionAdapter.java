import api.ConnectionMethod;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class DelegatingConnectionAdapter implements Connection {
    private final Connection connection;

    public DelegatingConnectionAdapter(Connection connection) {
        this.connection = connection;
    }

    public Connection getWrappedConnection() {
        doActionBeforeEachMethod();
        return connection;
    }

    @Override
    public Statement createStatement() throws SQLException {
        doActionBeforeEachMethod();
        return connection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        doActionBeforeEachMethod();
        return connection.nativeSQL(sql);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        doActionBeforeEachMethod();
        connection.setAutoCommit(autoCommit);
    }

    @Override
    public void commit() throws SQLException {
        doActionBeforeEachMethod();
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        doActionBeforeEachMethod(ConnectionMethod.CLOSE);
        connection.commit();
    }

    @Override
    public void close() throws SQLException {
        doActionBeforeEachMethod();
        connection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        doActionBeforeEachMethod();
        return connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getMetaData();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        doActionBeforeEachMethod();
        return connection.isReadOnly();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        doActionBeforeEachMethod();
        connection.setReadOnly(readOnly);
    }

    @Override
    public String getCatalog() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getCatalog();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        doActionBeforeEachMethod();
        connection.setCatalog(catalog);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getTransactionIsolation();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        doActionBeforeEachMethod();
        connection.setTransactionIsolation(level);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        doActionBeforeEachMethod();
        connection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        doActionBeforeEachMethod();
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        doActionBeforeEachMethod();
        connection.setTypeMap(map);
    }

    @Override
    public int getHoldability() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getHoldability();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        doActionBeforeEachMethod();
        connection.setHoldability(holdability);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        doActionBeforeEachMethod();
        return connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return connection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        doActionBeforeEachMethod();
        connection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        doActionBeforeEachMethod();
        connection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        doActionBeforeEachMethod();
        return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareCall(sql,resultSetType,resultSetConcurrency,resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql,autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql,columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        doActionBeforeEachMethod();
        return connection.prepareStatement(sql,columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        doActionBeforeEachMethod();
        return connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        doActionBeforeEachMethod();
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        doActionBeforeEachMethod();
        return connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        doActionBeforeEachMethod();
        return connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        doActionBeforeEachMethod();
        connection.setClientInfo(name,value);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        doActionBeforeEachMethod();
        return connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getClientInfo();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        doActionBeforeEachMethod();
        connection.setClientInfo(properties);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        doActionBeforeEachMethod();
        return connection.createArrayOf(typeName,elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        doActionBeforeEachMethod();
        return connection.createStruct(typeName,attributes);
    }

    @Override
    public String getSchema() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getSchema();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        doActionBeforeEachMethod();
        connection.setSchema(schema);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        doActionBeforeEachMethod();
        connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        doActionBeforeEachMethod();
        connection.getNetworkTimeout();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        doActionBeforeEachMethod();
        return connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        doActionBeforeEachMethod();
        if(iface.isInstance(this)){
            return (T) this;
        }
        return connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        doActionBeforeEachMethod();
        return iface.isInstance(this) || connection.isWrapperFor(iface);

    }

    protected void doActionBeforeEachMethod(ConnectionMethod method) {
    }

    protected void doActionBeforeEachMethod() {
        doActionBeforeEachMethod(ConnectionMethod.DEFAULT);
    }
}
