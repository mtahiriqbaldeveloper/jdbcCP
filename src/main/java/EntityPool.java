import api.EntityChecker;
import api.EntityFactory;
import api.EntityReleaser;
import api.NoOpEntityReleaser;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class EntityPool<T> {
    private static final Logger log = Logger.getLogger(EntityPool.class.getName());
    private static final int DEFAULT_INITIAL_POOL_SIZE = 0;
    private static final int DEFAULT_MAX_POOL_SIZE = 0;
    private static final int DEFAULT_PRIORITY_POOL_SIZE = 0;
    private static final int DEFAULT_CHECKOUT_TIMEOUT = -1;

    private final ArrayBlockingQueue<T> entities;
    private final Collection<EntityChecker<? super T>> checkers;
    private final EntityFactory<T> factory;
    private final int maxSize;
    private final int prioritySize;
    private final long defaultCheckoutTime;
    private final Semaphore entityPermits;
    private final boolean parallelCreation;
    private final AtomicInteger currentSize = new AtomicInteger();
    private final AtomicInteger waiting = new AtomicInteger();
    private final AtomicBoolean close = new AtomicBoolean();
    private EntityReleaser<? super T> releaser;
    // Leak detection fields
    private  ConcurrentHashMap<T, CheckoutInfo> checkedOutEntities = new ConcurrentHashMap<>();
    private  long leakDetectionThreshold;
    private  boolean leakDetectionEnabled;
    private  ScheduledExecutorService leakDetector;
    // Inner class to track checkout information
    private static class CheckoutInfo {
        final long checkoutTime;
        final String threadName;
        final StackTraceElement[] stackTrace;
        volatile boolean leakReported;

        CheckoutInfo() {
            this.checkoutTime = System.currentTimeMillis();
            this.threadName = Thread.currentThread().getName();
            this.stackTrace = Thread.currentThread().getStackTrace();
            this.leakReported = false;
        }
    }
    public EntityPool(Collection<EntityChecker<? super T>> checkers, EntityFactory<T> factory, EntityReleaser<? super T> releaser,
                      boolean parallelCreation, int maxSize, int prioritySize, long defaultCheckoutTime, int initialSize) throws IllegalArgumentException {
        this.checkers = Collections.unmodifiableCollection(checkers);
        this.factory = factory;
        this.releaser = releaser;
        this.maxSize = maxSize;
        this.parallelCreation = parallelCreation;
        this.prioritySize = prioritySize;
        this.defaultCheckoutTime = defaultCheckoutTime;
        this.entityPermits = new Semaphore(maxSize, true);
        this.entities = new ArrayBlockingQueue<>(maxSize, true);
        createEntitiesOnInitialization(initialSize, parallelCreation);
        // Leak detection setup
        this.leakDetectionThreshold = 50000;
        this.leakDetectionEnabled = true;

        if (leakDetectionEnabled) {
            this.leakDetector = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "EntityPool-LeakDetector");
                t.setDaemon(true);
                return t;
            });

            // Schedule leak detection to run every minute
            this.leakDetector.scheduleWithFixedDelay(this::detectLeaks,
                    60, 60, TimeUnit.SECONDS);
        } else {
            this.leakDetector = null;
        }
    }

    public static <T> EntityPoolBuilder<T> builder() {
        return new EntityPoolBuilder<>();
    }

    public boolean isCheckersConfigured() {
        return !this.checkers.isEmpty();
    }

    public int getIdleEntitiesNumber() {
        return this.entities.size();
    }

    private void createEntitiesOnInitialization(int initSize, boolean parallelCreation) {
        List<T> entities = new CopyOnWriteArrayList<>();

        StreamSupport.intStream(IntStream.range(0, initSize).spliterator(), parallelCreation)
                .forEach((__) -> EntityPoolContext.execPrioritized(() -> this.createSingleEntity(initSize, entities)));
        //Return all entities to the pool and finish initalization if everything is ok.
        if (entities.size() == initSize) {
            for (T entity : entities) {
                release(entity);
            }
            return;
        }
        if (entities.size() < initSize) {
            log.warning(String.format("Pool initialization: requested %d entities, created %d",
                    initSize, entities.size()));
        }
        doRelease(entities, String.format("inability to create necessary number of manage entities during entity pool initialization " +
                "(required %d, created %d)", initSize, entities.size()));
        throw new IllegalStateException(String.format("can't create necessary number of required manage entities within the entity pool " +
                "(required %d, actually created %d). used the follwoing entity factory: %s", initSize, entities.size(), factory));
    }


    private void createSingleEntity(int number, List<T> entities) {
        T entity;
        try {
            entity = this.getEntity(0L);
        } catch (InterruptedException e) {
            String message = String.format("create necessary number of managed entities during entity pool initialization (required %d, created %d) because calling thread interruption", number, entities.size());
            this.doRelease(entities, "inability to" + message);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message);
        }
        if (entity != null) {
            entities.add(entity);
        }
    }

    private void doRelease(Iterable<T> entities, String reason) {
        for (T entity : entities) {
            doRelease(entity, reason);
        }
    }

    private void doRelease(T entity, String reason) {
        try {
            this.releaser.release(entity);
        } catch (Exception var) {
            log.warning(var.getMessage());
            log.warning(String.format("unexpected expection occured on attempt to release entity '%s' via '%s'. The entity was released due to %s ", entity, this.releaser, reason));
        }
    }

    public T getEntity() throws InterruptedException {
        return getEntity(defaultCheckoutTime);
    }

    private T getEntity(long timout) throws InterruptedException, IllegalStateException {
        if (this.close.get()) {
            throw new IllegalStateException(String.format("Can't retrieve entity from the pool '%s'. Reason the pool is closed", this));
        } else {
            long start = System.currentTimeMillis();
            log.info("waiting threads" + start);
            try {
                return this.getEntityImpl(timout);
            } finally {
                log.info("stats accumulator");
            }
        }
    }

    public int getMaxPoolSize() {
        return maxSize;
    }

    private T getEntityImpl(long timout) throws InterruptedException {
        long endRequestTime = System.currentTimeMillis() + timout;

        if (!this.tryAcquire(timout)) {
            return null;
        }
        int maxAttemptNumber = 100;
        int i = 0;

        while (++i < maxAttemptNumber) {
            T entity = entities.poll();
            if (entity != null) {
                return entity;
            }
            int current = getCurrentSize();
            if (!currentSize.compareAndSet(current, current + 1)) {
                //retry in case another thread simultaneously created the entity
                continue;
            }
            try {
                entity = factory.create();
            } catch (Exception var1) {
                Exception e = var1;
                this.decrementCurrentSize();
                if (e instanceof IllegalStateException) {
                    log.warning("failed to create new entity, tried following entity factory : " + this.factory);
                } else {
                    log.warning(String.format("Failed to create entity using the factory '%s' factory has thrown unexpected exception '%s' ", this.factory, e));
                }
                if (timout > 0L && endRequestTime > System.currentTimeMillis()) {
                    continue;
                }
            }
            if (entity == null) {
                log.warning(String.format("Entity factory '%s' failed to create new manage entity - null is returned", this.factory));
                decrementCurrentSize();
                continue;
            }
            return entity;
        }
        log.warning(String.format("Potential live lock on manage entity creation via factory '%s'!!!! " +
                "Failed to create new entity in %d attempts", factory, maxAttemptNumber));

        return null;
    }

    private boolean tryAcquire(long timout) throws InterruptedException {
        int prioritizedPermits = EntityPoolContext.isPrioritized() ? 0 : this.prioritySize;
        boolean acquired = false;
        if (timout < 0L) {
            this.entityPermits.acquire(prioritizedPermits + 1);
            acquired = true;
        } else if (this.entityPermits.tryAcquire(prioritizedPermits + 1, timout, TimeUnit.MILLISECONDS)) {
            acquired = true;
        }
        if (acquired && prioritizedPermits > 0) {
            this.entityPermits.release(prioritizedPermits);
        }
        return acquired;
    }

    private void decrementCurrentSize() {
        this.currentSize.decrementAndGet();
    }

    public int getCurrentSize() {
        return currentSize.get();
    }

    public void release(T entity) {
        release(entity, false);
    }

    public void release(T entity, boolean check) {
        int current = getCurrentSize() - 1;
        if (!close.get() && current < getMaxPoolSize() && (!check || checkEntity(entity))) {
            entities.offer(entity);
            entityPermits.release();
        } else {
            doRelease(entity, String.format("exceeding max pool size: max='%d', current='%d'", getMaxPoolSize(), current));
            decrementCurrentSize();
        }
        log.info("remember stats");

    }

    public void close() {
        if (!close.compareAndSet(false, true)) {
            //another thread concurrently closed it.
            return;
        }
        T entity;
        while ((entity = entities.poll()) != null) {
            doRelease(entity, "closing pool");
            decrementCurrentSize();
        }
        log.info("record the stats");
    }

    /**
     * Checks whether the given entity is valid by running it through
     * all registered {@link EntityChecker}s. The entity is considered
     * valid only if every checker returns {@code true}. If any checker
     * returns {@code false} or throws an exception, this method will
     * log the failure and return {@code false}.
     *
     * <p><strong>Behavior details:</strong>
     * <ul>
     *   <li>Iterates over the {@code checkers} collection.</li>
     *   <li>Invokes {@code checker.check(entity)} for each.</li>
     *   <li>If a checker returns {@code true}, moves on to the next.</li>
     *   <li>If a checker returns {@code false}:
     *   </li>
     *   <li>If a checker throws any {@link Exception}:
     *     <ul>
     *       <li>Logs an ERROR message (treating the entity as broken)</li>
     *       <li>Immediately returns {@code false}</li>
     *     </ul>
     *   </li>
     *   <li>If all checkers return {@code true} without throwing, returns {@code true}.</li>
     * </ul>
     *
     * @param entity the entity to validate; must not be {@code null}
     * @return {@code true} if and only if every {@link EntityChecker} in
     * {@code checkers} returns {@code true} for this entity;
     * {@code false} otherwise
     */
    private boolean checkEntity(T entity) {
        for (EntityChecker<? super T> checker : checkers) {
            boolean validEntity = false;
            try {
                validEntity = checker.check(entity);
            } catch (Exception e) {
                log.log(Level.WARNING, String.format(
                        "Unexpected exception occurred on attempt to check if entity '%s' is still alive via '%s'",
                        entity, checker), e);
            }
            if (validEntity) {
                continue;
            }
            return false;
        }
        return true;
    }


    /**
     * Checks if it should be evicted (either pool size exceeded or failed validation)
     * If valid, puts it back in the queue
     * If invalid, releases it permanently
     */
    public void refresh() {

        List<T> processedEntities = new ArrayList<>(this.getIdleEntitiesNumber());
        int max = this.getMaxPoolSize();

        T entity = entities.poll();
        for (; entity != null && !processedEntities.contains(entity); entity = entities.poll()) {
            int current = getCurrentSize() - 1;
            boolean evictEntity = current >= max || !checkEntity(entity);
            if (!evictEntity) {
                processedEntities.add(entity);
                entities.offer(entity);
                continue;
            }
            doRelease(entity, current > max ? String.format("exceeding max pool size (%d current pool size is %d)", max, current)
                    : String.format("invalid entity detection during refresh (%s)", entity)
            );
            decrementCurrentSize();
        }
        if (entity != null) {
            entities.offer(entity);
        }
        log.info("remember stats");
    }
    private void detectLeaks() {
        if (!leakDetectionEnabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        List<T> leakedEntities = new ArrayList<>();

        for (Map.Entry<T, CheckoutInfo> entry : checkedOutEntities.entrySet()) {
            T entity = entry.getKey();
            CheckoutInfo info = entry.getValue();

            long holdTime = currentTime - info.checkoutTime;

            if (holdTime > leakDetectionThreshold) {
                if (!info.leakReported) {
                    // Log leak detection with stack trace
                    StringBuilder stackTrace = new StringBuilder();
                    stackTrace.append("Entity leak detected!\n");
                    stackTrace.append(String.format("Entity: %s\n", entity));
                    stackTrace.append(String.format("Checked out by thread: %s\n", info.threadName));
                    stackTrace.append(String.format("Hold time: %d ms (threshold: %d ms)\n", holdTime, leakDetectionThreshold));
                    stackTrace.append("Stack trace at checkout:\n");

                    for (StackTraceElement element : info.stackTrace) {
                        stackTrace.append("  at ").append(element.toString()).append("\n");
                    }

                    log.warning(stackTrace.toString());
                    info.leakReported = true;
                }

                // Optionally force-close leaked entities after a longer threshold
                if (holdTime > leakDetectionThreshold * 2) {
                    leakedEntities.add(entity);
                }
            }
        }

        // Force release entities that have been leaked for too long
        for (T leakedEntity : leakedEntities) {
            log.warning(String.format("Force releasing leaked entity: %s", leakedEntity));
            checkedOutEntities.remove(leakedEntity);
            doRelease(leakedEntity, "force release due to prolonged leak");
            decrementCurrentSize();
            entityPermits.release();
        }
    }
    public void debugPoolState() {
        System.out.println("=== Pool Debug Info ===");
        System.out.println("Current size: " + getCurrentSize());
        System.out.println("Idle entities: " + getIdleEntitiesNumber());
        System.out.println("Max pool size: " + getMaxPoolSize());
        System.out.println("Available permits: " + entityPermits.availablePermits());
    }

    public static final class EntityPoolBuilder<T> {
        private int initialSize = DEFAULT_INITIAL_POOL_SIZE;
        private int maxSize = DEFAULT_MAX_POOL_SIZE;
        private int prioritySize = DEFAULT_PRIORITY_POOL_SIZE;
        private long defaultCheckoutTimeout = DEFAULT_CHECKOUT_TIMEOUT;
        private EntityFactory<T> factory;
        private boolean parallelCreation;
        private EntityReleaser<? super T> releaser = new NoOpEntityReleaser<>();
        private final Collection<EntityChecker<? super T>> checkers = new ArrayList<>();

        private EntityPoolBuilder() {
        }

        public EntityPoolBuilder<T> factory(EntityFactory<T> factory) {
            this.factory = factory;
            return this;
        }

        public EntityPoolBuilder<T> initialSize(int initialSize) {
            if (initialSize <= 0) {
                throw new IllegalArgumentException(String.format("Invalid initial size: %d. the value should be positive", initialSize));
            }
            this.initialSize = initialSize;
            return this;
        }

        public EntityPoolBuilder<T> maxSize(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException(String.format("Invalid max size: %d. the value should be positive", maxSize));
            }
            this.maxSize = maxSize;
            return this;
        }

        public EntityPoolBuilder<T> prioritySize(int prioritySize) {
            if (prioritySize <= 0) {
                throw new IllegalArgumentException(String.format("Invalid priority size: %d. the value should be positive", prioritySize));
            }
            this.prioritySize = prioritySize;
            return this;
        }

        public EntityPoolBuilder<T> defaultCheckoutTime(long defaultCheckoutTimeout) {
            this.defaultCheckoutTimeout = defaultCheckoutTimeout;
            return this;
        }

        public EntityPoolBuilder<T> releaser(EntityReleaser<? super T> releaser) {
            this.releaser = releaser;
            return this;
        }

        public EntityPoolBuilder<T> parrelCreation(boolean parallelCreation) {
            this.parallelCreation = parallelCreation;
            return this;
        }

        public EntityPoolBuilder<T> addCheckers(EntityChecker<? super T>... checkers) {
            this.checkers.addAll(Arrays.asList(checkers));
            return this;
        }

        public EntityPoolBuilder<T> setCheckers(EntityChecker<? super T>... checkers) {
            this.checkers.clear();
            return this.addCheckers(checkers);
        }

        public EntityPool<T> build() {
            if (this.factory == null) {
                throw new IllegalArgumentException("can't create entitypool with null entity factory");
            } else if (this.releaser == null) {
                throw new IllegalArgumentException(String.format("can't create entitypool with null entity releaser (provided entity factory is %s)", this.factory));
            } else {
                if (this.initialSize > this.maxSize) {
                    EntityPool.log.warning(String.format("Initial pool size %d is greater than max pool size %d. Max pool size value will be used instead. check config", this.initialSize, this.maxSize));
                    this.initialSize = this.maxSize;
                }
                return new EntityPool<>(checkers, factory, releaser, parallelCreation,
                        maxSize, prioritySize, defaultCheckoutTimeout, initialSize);
            }
        }
    }

}
