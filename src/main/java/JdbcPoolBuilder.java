import api.EntityChecker;
import api.EntityConverter;
import api.EntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;


public class JdbcPoolBuilder {
    private static final Logger log = LoggerFactory.getLogger(JdbcPoolBuilder.class);
    private final List<EntityConverter<Connection, Connection>> connectionConverters = new ArrayList<>();
    private final Collection<ApplicationStatusProvider> statusProviders = new ArrayList<>();
    private final DefaultConnectionChecker defaultConnectionChecker;
    private final EntityPool.EntityPoolBuilder<PoolConnection> entityPoolBuilder;
    private final Properties properties;
    private String url;
    private String password;
    private String login;
    private String driverClass;
    private JdbcPool jdbcPool;
    private ThreadGroup threadGroup;
    private StatementCacheConnectionFactory statementCacheConnectionFactory;
    private boolean useConnectionStatementCache = true;
    private boolean autocloseStatements = true;
    private EntityFactory<Connection> connectionFactory;
    private long idleConnectionCheckPeriod = 600000;
    private long idleStatementsCheckPeriod;
    private long statementIdlePeriod;
    private boolean traceEnabled;
    private MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private boolean jmxInitialized;
    private String jmxDomain = "com.brotech.jdbc";
    private long aggregationPeriod;
    private boolean checkConnectionThreadOwnerShip;
    private boolean useLogModeToCheckThreadOwnerShip;

    @SuppressWarnings("unchecked")
    public JdbcPoolBuilder(Properties properties) {
        this.properties = properties;
        this.aggregationPeriod = 100L;
        this.defaultConnectionChecker = new DefaultConnectionChecker();
        this.entityPoolBuilder = EntityPool.<PoolConnection>builder()
                .releaser(new DefaultConnectionReleaser())
                .addCheckers(new EntityChecker[]{defaultConnectionChecker}).parrelCreation(true)
                .initialSize(5)
                .maxSize(10)
                .parrelCreation(true)
                .defaultCheckoutTime(2000);
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public void setThreadGroup(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    public void setAutocloseStatements(boolean autocloseStatements) {
        this.autocloseStatements = autocloseStatements;
    }


    public void setIdleConnectionCheckPeriod(long idleConnectionCheckPeriod) {
        this.idleConnectionCheckPeriod = idleConnectionCheckPeriod;
    }


    public void setIdleStatementsCheckPeriod(long idleStatementsCheckPeriod) {
        this.idleStatementsCheckPeriod = idleStatementsCheckPeriod;
    }


    public void setStatementIdlePeriod(long statementIdlePeriod) {
        this.statementIdlePeriod = statementIdlePeriod;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public MBeanServer getmBeanServer() {
        return mBeanServer;
    }

    public void setmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public boolean isJmxInitialized() {
        return jmxInitialized;
    }

    public void setJmxInitialized(boolean jmxInitialized) {
        this.jmxInitialized = jmxInitialized;
    }

    public String getJmxDomain() {
        return jmxDomain;
    }

    public void setJmxDomain(String jmxDomain) {
        this.jmxDomain = jmxDomain;
    }

    public long getAggregationPeriod() {
        return aggregationPeriod;
    }

    public void setAggregationPeriod(long aggregationPeriod) {
        this.aggregationPeriod = aggregationPeriod;
    }

    public DefaultConnectionChecker getDefaultConnectionChecker() {
        return defaultConnectionChecker;
    }

    public EntityPool.EntityPoolBuilder<PoolConnection> getEntityPoolBuilder() {
        return entityPoolBuilder;
    }

    public void setDefaultCheckoutTime(long timeout) {
        entityPoolBuilder.defaultCheckoutTime(timeout);
    }

    public boolean isCheckConnectionThreadOwnerShip() {
        return checkConnectionThreadOwnerShip;
    }

    public void setCheckConnectionThreadOwnerShip(boolean checkConnectionThreadOwnerShip) {
        this.checkConnectionThreadOwnerShip = checkConnectionThreadOwnerShip;
    }

    public boolean isUseLogModeToCheckThreadOwnerShip() {
        return useLogModeToCheckThreadOwnerShip;
    }

    public void setUseLogModeToCheckThreadOwnerShip(boolean useLogModeToCheckThreadOwnerShip) {
        this.useLogModeToCheckThreadOwnerShip = useLogModeToCheckThreadOwnerShip;
    }

    public JdbcPool build() throws IllegalAccessException {
        if (jdbcPool != null) {
            return jdbcPool;
        }
        EntityFactory<Connection> factory = createFactory();
        EntityFactory<PoolConnection> factoryToUse = wrapFactory(factory);
        if (statementCacheConnectionFactory != null) {
            entityPoolBuilder.addCheckers(new EntityChecker[]{this.statementCacheConnectionFactory});
        }
        EntityPool<PoolConnection> entityPool = entityPoolBuilder.factory(factoryToUse).build();
        this.configurePoolRefresh(entityPool);
        this.jdbcPool = new JdbcPool(entityPool, this.statementCacheConnectionFactory);
        this.jdbcPool.setApplicationStatusProviders(this.statusProviders);
        return jdbcPool;
    }

    private void configurePoolRefresh(final EntityPool<PoolConnection> pool) {
        if (this.idleConnectionCheckPeriod > 0L) {
            if (!pool.isCheckersConfigured()) {
                log.error("idle connection check period is defined but no checkers are configured! no periodic check will be performed");
            }
            if (this.threadGroup == null) {
                this.threadGroup = Thread.currentThread().getThreadGroup();
            }
            Thread checkThread = new Thread(this.threadGroup, "jdbcPoolIdleConnectionCheckThread") {
                public void run() {
                    long startTime = System.currentTimeMillis();
                    while (!this.isInterrupted()) {
                        long timeToSleep = startTime + JdbcPoolBuilder.this.idleConnectionCheckPeriod - System.currentTimeMillis();
                        if (timeToSleep > 0L) {
                            try {
                                Thread.sleep(timeToSleep);
                            } catch (InterruptedException var6) {
                                String message = String.format("jdbcPoolIdleConnectionCheckThread is interrupted! '%s'", var6);
                                log.error(message);
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        startTime = System.currentTimeMillis();
                        pool.refresh();
                    }
                }
            };
            checkThread.setDaemon(true);
            checkThread.start();
        }
    }

    private EntityFactory<PoolConnection> wrapFactory(EntityFactory<Connection> factory) {
        if (useConnectionStatementCache) {
            statementCacheConnectionFactory = new StatementCacheConnectionFactory(idleConnectionCheckPeriod, statementIdlePeriod);
            connectionConverters.add(new EntityConverter<Connection, Connection>() {
                @Override
                public boolean canConvert(Connection var1) {
                    return true;
                }

                @Override
                public Connection convert(Connection entity) throws IllegalArgumentException {
                    return statementCacheConnectionFactory.getConnection(entity);
                }
            });
        }
        final EntityFactory<JdbcPool> poolProvider = () -> jdbcPool;
        return () -> {
            Connection result = factory.create();
            for (EntityConverter<Connection, Connection> converter : connectionConverters) {
                if (converter.canConvert(result)) {
                    result = converter.convert(result);
                }
            }
            PoolConnection poolConnection = new PoolConnectionImpl(poolProvider, result, autocloseStatements);
            if (checkConnectionThreadOwnerShip) {
                poolConnection = new ThreadOwnedPoolConnection(poolConnection, useLogModeToCheckThreadOwnerShip);
            }
            return poolConnection;
        };
    }

    private EntityFactory<Connection> createFactory() throws IllegalAccessException {
        if (connectionFactory == null) {
            log.info("creating the jdbc connection factory");
            return createJdbcConnectionFactory();
        }
        if (isJdbcParamsEmpty()) {
            log.error("some parameters are missing");
        }
        return connectionFactory;
    }

    private boolean isJdbcParamsEmpty() {
        return driverClass == null && login == null && password == null && url == null;
    }

    private EntityFactory<Connection> createJdbcConnectionFactory() throws IllegalAccessException {
        return new DriverManagerConnectionFactory(driverClass, url, properties);
    }
}
