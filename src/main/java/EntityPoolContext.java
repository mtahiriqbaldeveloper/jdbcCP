import java.util.concurrent.Callable;

public class EntityPoolContext {
    private static final ThreadLocal<Boolean> prioritized = ThreadLocal.withInitial(()-> Boolean.FALSE);
    private EntityPoolContext(){}
    public static Boolean isPrioritized(){
        return prioritized.get();
    }
    public static void execPrioritized(Runnable runnable){
        execPrioritized(()->{
            runnable.run();
            return null;
        });
    }
    public static <T> T execPrioritized(Callable<T> callable){
        T result = null;
        boolean isNested = prioritized.get();
        try {
            if(!isNested){
                prioritized.set(Boolean.TRUE);
            }
            result = callable.call();
        } catch (Exception e) {
            //ExpectionUtil can be used here
            throw new RuntimeException(e);
        }finally {
            if(!isNested){
                prioritized.set(Boolean.FALSE);
            }
        }
        return result;
    }
}
