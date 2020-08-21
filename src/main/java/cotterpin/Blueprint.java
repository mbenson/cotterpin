package cotterpin;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.Typed;

public interface Blueprint<T, S extends Blueprint<T, S>> extends Supplier<T> {

    public interface Child<T, U, P extends Blueprint<U, P>, S extends Child<T, U, P, S>> extends Blueprint<T, S> {

        P onto(BiConsumer<? super U, ? super T> mutator);

        default <C extends Collection<? super T>> P addTo(Function<? super U, C> coll) {
            return addTo(coll, null);
        }

        <C extends Collection<? super T>> P addTo(Function<? super U, C> coll, IfNull<U, C> ifNull);

        default <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map) {
            return into(map, null);
        }

        <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map,
                IfNull<U, M> ifNull);
    }

    public interface IntoMap<K, V, U, P extends Blueprint<U, P>> {

        P at(K key);
    }

    public interface Mutator<T, U, P extends Blueprint<U, P>, S extends Mutator<T, U, P, S>> extends Blueprint<T, S> {

        P onto(Function<? super U, ? extends T> prop);
    }

    <X, C extends Child<X, T, S, C>> C child(Supplier<X> c);

    default <X, C extends Child<X, T, S, C>> C child(X c) {
        return child(() -> c);
    }

    <X, M extends Mutator<X, T, S, M>> M mutate(Typed<X> type);

    <X, M extends Mutator<X, T, S, M>> M mutate(Class<X> type);
}
