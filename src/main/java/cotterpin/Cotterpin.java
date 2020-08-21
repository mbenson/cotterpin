package cotterpin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.Typed;

import cotterpin.Blueprint.Child;
import cotterpin.Blueprint.IntoMap;
import cotterpin.Blueprint.Mutator;

public class Cotterpin {

    private static class TemplateImpl<T, S extends TemplateImpl<T, S>> implements Blueprint<T, S> {

        final List<Consumer<? super T>> mutations = new ArrayList<>();
        Supplier<T> target;

        TemplateImpl(Supplier<T> target) {
            this.target = target;
        }

        @Override
        public T get() {
            T t = Optional.ofNullable(target).map(Supplier::get).orElseThrow(IllegalStateException::new);
            mutations.forEach(m -> m.accept(t));
            return t;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, C extends Child<X, T, S, C>> C child(Supplier<X> c) {
            return (C) new ChildImpl(Objects.requireNonNull(c), this);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, M extends Mutator<X, T, S, M>> M mutate(Typed<X> type) {
            return (M) new MutatorImpl(this);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, M extends Mutator<X, T, S, M>> M mutate(Class<X> type) {
            return (M) new MutatorImpl(this);
        }

        void add(Consumer<? super T> mutation) {
            mutations.add(mutation);
        }
    }

    private static class ChildImpl<T, U, P extends TemplateImpl<U, P>, S extends ChildImpl<T, U, P, S>>
            extends TemplateImpl<T, S> implements Child<T, U, P, S> {

        P parent;

        ChildImpl(Supplier<T> target, P parent) {
            super(target);
            this.parent = parent;
        }

        @Override
        public P onto(BiConsumer<? super U, ? super T> mutator) {
            ensureOpen();
            parent.add(p -> mutator.accept(p, get()));
            return close();
        }

        @Override
        public <C extends Collection<? super T>> P addTo(Function<? super U, C> coll) {
            ensureOpen();
            parent.add(p -> coll.apply(p).add(get()));
            return close();
        }

        @Override
        public <C extends Collection<? super T>> P addTo(Function<? super U, C> coll, IfNull<U, C> ifNull) {
            ensureOpen();
            parent.add(p -> obtainFrom(p, coll, ifNull).add(get()));
            return close();
        }

        @Override
        public <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map,
                IfNull<U, M> ifNull) {
            ensureOpen();
            try {
                return new IntoMapImpl<>(this, map, parent, ifNull);
            } finally {
                parent = null;
            }
        }

        private void ensureOpen() {
            Validate.validState(parent != null, "closed");
        }

        private P close() {
            try {
                return parent;
            } finally {
                parent = null;
            }
        }
    }

    private static class MutatorImpl<T, U, P extends TemplateImpl<U, P>, S extends MutatorImpl<T, U, P, S>>
            extends TemplateImpl<T, S> implements Mutator<T, U, P, S> {

        P parent;

        MutatorImpl(P parent) {
            super(null);
            this.parent = parent;
        }

        @Override
        public P onto(Function<? super U, ? extends T> prop) {
            parent.add((Consumer<? super U>) p -> {
                T t = prop.apply(p);
                mutations.forEach(m -> m.accept(t));
            });
            try {
                return parent;
            } finally {
                parent = null;
            }
        }

        @Override
        public T get() {
            throw new UnsupportedOperationException();
        }
    }

    private static class IntoMapImpl<K, V, U, P extends TemplateImpl<U, P>, M extends Map<? super K, ? super V>>
            implements IntoMap<K, V, U, P> {

        Supplier<V> value;
        Function<? super U, M> map;
        P parent;
        IfNull<U, M> ifNull;

        IntoMapImpl(Supplier<V> value, Function<? super U, M> map, P parent, IfNull<U, M> ifNull) {
            this.value = value;
            this.map = map;
            this.parent = parent;
            this.ifNull = ifNull;
        }

        @Override
        public P at(K key) {
            Validate.validState(parent != null);
            parent.add((Consumer<U>) u -> obtainFrom(u, map, ifNull).put(key, value.get()));
            try {
                return parent;
            } finally {
                parent = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, R extends Blueprint<T, R>> R build(Supplier<T> t) {
        return (R) new TemplateImpl<>(t);
    }

    public static <T, R extends Blueprint<T, R>> R builder(T t) {
        return build(() -> t);
    }

    public static <P, T> IfNull<P, T> ifNull(BiConsumer<? super P, ? super T> store, Supplier<T> create) {
        return new IfNull<P, T>(store, create);
    }

    private static <P, T> T obtainFrom(P parent, Function<? super P, ? extends T> retrieve, IfNull<P, T> ifNull) {
        T result = retrieve.apply(parent);
        if (result == null && ifNull != null) {
            result = ifNull.create.get();
            ifNull.record.accept(parent, result);
        }
        return result;
    }

    private Cotterpin() {
    }
}
