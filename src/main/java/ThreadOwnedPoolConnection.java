import api.ConnectionMethod;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThreadOwnedPoolConnection extends PoolConnectionDelegate{
    private final Logger log = Logger.getLogger(ThreadOwnedPoolConnection.class.getName());
    private final boolean logMode;
    private boolean isClosed;
    private static final Set<ConnectionMethod> skipCheckMethods = Stream.of(
            ConnectionMethod.IS_VALID, ConnectionMethod.UNCHECKED_GET_WRAPPED_CONNECTION)
            .collect(Collectors.toSet());
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    public ThreadOwnedPoolConnection(PoolConnection connection, boolean logMode) {
        super(connection);
        this.logMode = logMode;
    }

    @Override
    protected void doActionBeforeEachMethod(ConnectionMethod method) {
        readLock.lock();
        try {
            checkOwnership(method);
        }finally {
            readLock.unlock();
        }
    }

    private void checkOwnership(ConnectionMethod method) {
        if(skipCheckMethods.contains(method) || isCloseInvodkedOnClosedConnection(method)){
            return;
        }
        Thread ownerThread = owner.get();
        if(!Thread.currentThread().equals(ownerThread)){
            StringBuilder builder = new StringBuilder();
            builder.append("[db connection issue] a thread which isn't owner of the target connection")
                    .append("tries to invoke method on it, where owner is : ").append(ownerThread);
            if(ownerThread != null){
                builder.append("\nConnection acquired by owner at: \n");
                for (StackTraceElement traceElement: ownerThread.getStackTrace()){
                    builder.append("\tat").append(traceElement).append('\n');
                }
            }
            IllegalStateException e = new IllegalStateException(builder.toString());
            if(logMode){
                log.log(Level.WARNING,e.toString());
                log.warning(e.getMessage());
            }else {
                throw e;
            }
        }
    }

    private boolean isCloseInvodkedOnClosedConnection(ConnectionMethod method){
        return ConnectionMethod.CLOSE.equals(method) && owner.get() == null && isClosed;
    }

    @Override
    public void grab() {
        writeLock.lock();
        try {
            owner.compareAndSet(null,Thread.currentThread());
            super.grab();
            isClosed = false;
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws SQLException {
        try {
            super.close();
            owner.set(null);
            isClosed = true;
        }finally {
            writeLock.unlock();
        }
    }
}
