import api.EntityChecker;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatementCacheConnectionFactory implements EntityChecker<PoolConnection> {
    private static final Logger log = Logger.getLogger(StatementCacheConnectionFactory.class.getName());

    private final ConcurrentHashMap<StatementCacheConnection,Long> connectionsAndCheckTime = new ConcurrentHashMap<>();
    private final long idleStatementCheckPeriod;
    private final long statementIdlePeriod;

    public StatementCacheConnectionFactory(long idleStatementCheckPeriod, long statementIdlePeriod) {
        this.idleStatementCheckPeriod = idleStatementCheckPeriod;
        this.statementIdlePeriod = statementIdlePeriod;
    }
    public StatementCacheConnection getConnection(Connection underlying){
        StatementCacheConnection connection = new StatementCacheConnection(this, underlying);
        connectionsAndCheckTime.put(connection,System.currentTimeMillis());
        return connection;
    }
    void connectionClosed(StatementCacheConnection statementCacheConnection){
        connectionsAndCheckTime.remove(statementCacheConnection);
    }
    public void dropOldStatements(){
        long dropStatementNotUsedSince = System.currentTimeMillis()-statementIdlePeriod;
        for(StatementCacheConnection connection : connectionsAndCheckTime.keySet()){
            connection.dropRecentlyNotUsedFromCache(dropStatementNotUsedSince);
        }
    }
    public int getCachedStatementsCount(){
        int result = 0;
        for (StatementCacheConnection connection: connectionsAndCheckTime.keySet()){
            result +=connection.getCachedStatementsCount();
        }
        return result;
    }

    @Override
    public boolean check(PoolConnection poolConnection) {
        Connection underlying = poolConnection.uncheckGetWrappedConnection();
        if (underlying instanceof StatementCacheConnection){
            StatementCacheConnection statementCacheConnection = (StatementCacheConnection) underlying;
            Long lastCheckTime = connectionsAndCheckTime.get(statementCacheConnection);
            long now = System.currentTimeMillis();
            if(lastCheckTime + idleStatementCheckPeriod < now){
                try {
                    statementCacheConnection.dropRecentlyNotUsedFromCache(now-statementIdlePeriod);
                    connectionsAndCheckTime.put(statementCacheConnection,now);
                }catch (Exception exception){
                    log.log(Level.WARNING, " error during releasing old statements");
                    log.warning(exception.getMessage());
                    return false;
                }
            }
        }
        return true;
    }
}
