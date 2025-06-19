package api;

public interface EntityConverter<I,O> {
    boolean canConvert(I var1);
    O convert(I var1) throws IllegalArgumentException;
}
