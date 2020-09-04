/*
 *  Copyright the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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

/**
 * Entry point.
 */
public class Cotterpin {

    private static class BlueprintImpl<T, S extends BlueprintImpl<T, S>> implements Blueprint<T, S> {

        final List<Consumer<? super T>> mutations = new ArrayList<>();
        Supplier<T> target;

        BlueprintImpl(Supplier<T> target) {
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

        @Override
        public Supplier<T> singleton() {
            return new Supplier<T>() {
                volatile Supplier<T> blueprint = BlueprintImpl.this;
                volatile T value;

                @Override
                public T get() {
                    if (blueprint != null) {
                        synchronized (this) {
                            if (blueprint != null) {
                                value = blueprint.get();
                                blueprint = null;
                            }
                        }
                    }
                    return value;
                }
            };
        }

        void add(Consumer<? super T> mutation) {
            mutations.add(mutation);
        }
    }

    private static class RootImpl<T, S extends RootImpl<T, S>> extends BlueprintImpl<T, S> implements Blueprint.Root<T, S> {

        RootImpl(Supplier<T> target) {
            super(target);
        }

        @Override
        public <TT, SS extends Root<TT, SS>> SS map(Function<? super T, ? extends TT> xform) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final SS result = (SS) new RootImpl((Supplier<TT>) () -> Objects.requireNonNull(xform).apply(get()));
            return result;
        }
    }

    private static class ChildImpl<T, U, P extends BlueprintImpl<U, P>, S extends ChildImpl<T, U, P, S>>
            extends BlueprintImpl<T, S> implements Child<T, U, P, S> {

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

        @Override
        public <TT, SS extends Child<TT, U, P, SS>> SS map(Function<? super T, ? extends TT> xform) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final SS result = (SS) new ChildImpl((Supplier<TT>) () -> xform.apply(get()), parent);
            return result;
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

    private static class MutatorImpl<T, U, P extends BlueprintImpl<U, P>, S extends MutatorImpl<T, U, P, S>>
            extends BlueprintImpl<T, S> implements Mutator<T, U, P, S> {

        P parent;

        MutatorImpl(P parent) {
            super(null);
            this.parent = parent;
        }

        @Override
        public P onto(Function<? super U, ? extends T> prop, IfNull<U, T> ifNull) {
            parent.add((Consumer<? super U>) p -> {
                T t = obtainFrom(p, prop, ifNull);
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

    private static class IntoMapImpl<K, V, U, P extends BlueprintImpl<U, P>, M extends Map<? super K, ? super V>>
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

    /**
     * Begin to build a {@link Blueprint}.
     * 
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value {@link Supplier}
     * @return R
     */
    @SuppressWarnings("unchecked")
    public static <T, R extends Blueprint.Root<T, R>> R build(Supplier<T> t) {
        return (R) new RootImpl<>(t);
    }

    /**
     * Begin to build a {@link Blueprint}.
     * 
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R build(T t) {
        return build(() -> t);
    }

    /**
     * Create an {@link IfNull} object. It is recommended that this method be
     * {@code static}ally imported in the interest of improving the fluent
     * experience.
     * 
     * @param <P>    parent type
     * @param <T>    child type
     * @param record {@link BiConsumer} to record onto a parent the child object
     *               created by {@code create}
     * @param create {@link Supplier} of {@code T}
     * @return {@link IfNull}
     */
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
