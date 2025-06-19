package api;

public class NoOpEntityReleaser <T> implements EntityReleaser<T> {
    public NoOpEntityReleaser() {
    }

    @Override
    public void release(T var1) {

    }
}
