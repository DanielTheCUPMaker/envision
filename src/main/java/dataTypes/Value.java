package dataTypes;

public interface Value<T> {
    public void set(T arg);
    public T get();
    public Value sum(T arg);
    public Value mul(T arg);
    public Value sub(T arg);
}
