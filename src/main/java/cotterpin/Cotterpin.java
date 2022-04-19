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

import static cotterpin.BuildStrategy.prototype;
import static cotterpin.BuildStrategy.singleton;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.reflect.Typed;

import cotterpin.Blueprint.Child;
import cotterpin.Blueprint.IntoMap;
import cotterpin.Blueprint.Mutator;
import cotterpin.Blueprint.Root;
import cotterpin.BlueprintLike.ForEach;

/**
 * Entry point.
 */
public class Cotterpin {

    private static class ChildStrategyManager implements ChildStrategy {
        final ChildStrategy inherited;
        ChildStrategy current;

        ChildStrategyManager(ChildStrategy inherited) {
            this.inherited = inherited;
            this.current = inherited;
        }

        void adopt(ChildStrategy... childStrategies) {
            ChildStrategy s = inherited;
            for (ChildStrategy childStrategy : childStrategies) {
                s = childStrategy.then(s);
            }
            synchronized (this) {
                current = s;
            }
        }

        @Override
        public <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer) {
            return current.apply(cmer);
        }
    }

    @SuppressWarnings("unchecked")
    private static class BlueprintLikeImpl<T, S extends BlueprintLikeImpl<T, S>> implements BlueprintLike<T, S> {
        final BuildStrategy<T> buildStrategy;
        final ChildStrategyManager children;

        BlueprintLikeImpl(BuildStrategy<T> buildStrategy, Supplier<T> target, ChildStrategy childStrategy) {
            this.buildStrategy = buildStrategy;
            buildStrategy.initialize(target);
            children = new ChildStrategyManager(childStrategy);
        }

        @Override
        public S times(int times, ObjIntConsumer<S> body) {
            iterate(times, bindTo((S) this, body));
            return (S) this;
        }

        @Override
        public <X> ForEach<X, S> each(Iterable<X> values) {
            return new ForEachImpl<>((S) this, values);
        }

        @Override
        public S then(Consumer<? super T> mutation) {
            buildStrategy.apply(mutation);
            return (S) this;
        }

        @Override
        public S strategy(ChildStrategy... strategies) {
            children.adopt(strategies);
            return (S) this;
        }
    }

    private static class ForEachImpl<E, B extends BlueprintLike<?, B>> implements ForEach<E, B> {
        final B parent;
        final Iterable<E> values;

        ForEachImpl(B parent, Iterable<E> values) {
            this.parent = Objects.requireNonNull(parent);
            this.values = Objects.requireNonNull(values);
        }

        @Override
        public B apply(BiConsumer<E, B> body) {
            values.forEach(v -> body.accept(v, parent));
            return parent;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class BlueprintImpl<T, S extends BlueprintImpl<T, S>> extends BlueprintLikeImpl<T, S>
            implements Blueprint<T, S> {

        BlueprintImpl(BuildStrategy<T> buildStrategy, Supplier<T> target, ChildStrategy childStrategy) {
            super(buildStrategy, target, childStrategy);
        }

        @Override
        public <X, C extends Child<X, T, S, C>> C child(Supplier<X> c) {
            return (C) new ChildImpl(buildStrategy.child(), Objects.requireNonNull(c), this, children.current);
        }

        @Override
        public <X, M extends Mutator<X, T, S, M>> M mutate(Typed<X> type) {
            return (M) new MutatorImpl(buildStrategy.child(), this, children.current);
        }

        @Override
        public <N extends WildChild<T, S, N>> N nul() {
            final ChildStrategyManager _children = new ChildStrategyManager(children.current);
            final MutableBoolean open = new MutableBoolean(Boolean.TRUE);

            return (N) new WildChild<T, S, N>() {

                @Override
                public <X> S onto(BiConsumer<? super T, ? super X> mutator) {
                    ensureOpen();
                    then(p -> children.apply(mutator).accept(p, null));
                    return close();
                }

                @Override
                public S addTo(Function<? super T, Collection<?>> coll,
                        ComponentStrategy<T, ? extends Collection<?>> strategy) {
                    ensureOpen();

                    final Function<T, Collection<?>> x = strategy.apply((Function) coll);

                    BiConsumer<T, ?> cmer = children.apply((u, t) -> ((Collection) x.apply(u)).add(t));
                    then(p -> cmer.accept(p, null));
                    return close();
                }

                @Override
                public <K, M extends Map<? super K, ?>> IntoMap<K, ?, T, S> into(Function<? super T, M> map,
                        ComponentStrategy<T, ? extends M> strategy) {
                    ensureOpen();
                    try {
                        final Function<T, M> m = strategy.apply((Function) map);
                        return new IntoMapImpl(() -> null, m, BlueprintImpl.this, children.current);
                    } finally {
                        close();
                    }
                }

                @Override
                public N strategy(ChildStrategy... strategies) {
                    _children.adopt(strategies);
                    return (N) this;
                }

                private synchronized void ensureOpen() {
                    Validate.validState(open.booleanValue());
                }

                private synchronized S close() {
                    try {
                        return (S) BlueprintImpl.this;
                    } finally {
                        open.setFalse();
                    }
                }
            };
        }
    }

    private static class RootImpl<T, S extends RootImpl<T, S>> extends BlueprintImpl<T, S>
            implements Blueprint.Root<T, S> {

        RootImpl(BuildStrategy<T> buildStrategy, Supplier<T> target) {
            super(buildStrategy, target, ChildStrategy.DEFAULT);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <TT, SS extends Root<TT, SS>> SS map(Function<? super T, ? extends TT> xform) {
            return (SS) new RootImpl(buildStrategy.child(), () -> Objects.requireNonNull(xform).apply(get()));
        }

        @Override
        public T get() {
            return buildStrategy.get();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class OfCollectionImpl<E, C extends Collection<E>, S extends OfCollectionImpl<E, C, S>>
            extends BlueprintLikeImpl<C, S> implements Blueprint.OfCollection<E, C, S> {

        OfCollectionImpl(BuildStrategy<C> buildStrategy, Supplier<C> c) {
            super(buildStrategy, c, ChildStrategy.DEFAULT);
        }

        @Override
        public C get() {
            return buildStrategy.get();
        }

        @Override
        public <R extends Blueprint.OfCollectionElement<E, C, S, R>> R element(Supplier<E> e) {
            return (R) new OfCollectionElementImpl(buildStrategy.child(), e, this, children.current);
        }

        @Override
        public <T, SS extends Root<T, SS>> SS map(Function<? super C, ? extends T> xform) {
            return (SS) new RootImpl(buildStrategy.child(), () -> Objects.requireNonNull(xform).apply(get()));
        }
    }

    private static class OfCollectionElementImpl<E, C extends Collection<E>, P extends Blueprint.OfCollection<E, C, P>, S extends OfCollectionElementImpl<E, C, P, S>>
            extends BlueprintImpl<E, S> implements Blueprint.OfCollectionElement<E, C, P, S>, Supplier<E> {

        P parent;

        OfCollectionElementImpl(BuildStrategy<E> buildStrategy, Supplier<E> target, P parent,
                ChildStrategy childStrategy) {
            super(buildStrategy, target, childStrategy);
            this.parent = parent;
        }

        @Override
        public P add() {
            Validate.validState(parent != null);
            try {
                BiConsumer<C, E> add = Collection::add;
                parent.then(c -> children.apply(add).accept(c, get()));
                return parent;
            } finally {
                parent = null;
            }
        }

        @Override
        public E get() {
            return buildStrategy.get();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class OfMapImpl<K, V, M extends Map<K, V>, S extends OfMapImpl<K, V, M, S>>
            extends BlueprintLikeImpl<M, S> implements Blueprint.OfMap<K, V, M, S> {

        OfMapImpl(BuildStrategy<M> buildStrategy, Supplier<M> m) {
            super(buildStrategy, m, ChildStrategy.DEFAULT);
        }

        @Override
        public M get() {
            return buildStrategy.get();
        }

        @Override
        public <R extends Blueprint.OfMapEntry<K, V, M, S, R>> R value(Supplier<V> v) {
            return (R) new OfMapEntryImpl(buildStrategy.child(), v, this, children.current);
        }

        @Override
        public <T, SS extends Root<T, SS>> SS map(Function<? super M, ? extends T> xform) {
            return (SS) new RootImpl(buildStrategy.child(), () -> Objects.requireNonNull(xform).apply(get()));
        }
    }

    private static class OfMapEntryImpl<K, V, M extends Map<K, V>, P extends Blueprint.OfMap<K, V, M, P>, S extends OfMapEntryImpl<K, V, M, P, S>>
            extends BlueprintImpl<V, S> implements Blueprint.OfMapEntry<K, V, M, P, S>, Supplier<V> {

        P parent;

        OfMapEntryImpl(BuildStrategy<V> buildStrategy, Supplier<V> target, P parent, ChildStrategy childStrategy) {
            super(buildStrategy, target, childStrategy);
            this.parent = parent;
        }

        @Override
        public P at(Supplier<K> key) {
            Validate.validState(parent != null);
            try {
                BiConsumer<M, V> put = (m, v) -> m.put(key.get(), v);
                parent.then(m -> children.apply(put).accept(m, get()));
                return parent;
            } finally {
                parent = null;
            }
        }

        @Override
        public V get() {
            return buildStrategy.get();
        }
    }

    private static class ChildImpl<T, U, P extends BlueprintImpl<U, P>, S extends ChildImpl<T, U, P, S>>
            extends BlueprintImpl<T, S> implements Child<T, U, P, S>, Supplier<T> {

        P parent;

        ChildImpl(BuildStrategy<T> buildStrategy, Supplier<T> target, P parent, ChildStrategy childStrategy) {
            super(buildStrategy, target, childStrategy);
            this.parent = parent;
        }

        @Override
        public P onto(BiConsumer<? super U, ? super T> mutator) {
            ensureOpen();
            parent.then(p -> children.apply(mutator).accept(p, get()));
            return close();
        }

        @Override
        public <C extends Collection<? super T>> P addTo(Function<? super U, C> coll,
                ComponentStrategy<U, C> strategy) {
            ensureOpen();

            @SuppressWarnings("unchecked")
            final Function<U, C> x = strategy.apply((Function<U, C>) coll);

            final BiConsumer<U, T> cmer = children.apply((u, t) -> x.apply(u).add(t));
            parent.then(p -> cmer.accept(p, get()));
            return close();
        }

        @Override
        public <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map,
                ComponentStrategy<U, M> strategy) {
            ensureOpen();
            try {
                @SuppressWarnings("unchecked")
                final Function<U, M> m = strategy.apply((Function<U, M>) map);
                return new IntoMapImpl<>(this, m, parent, children.current);
            } finally {
                close();
            }
        }

        @Override
        public <TT, SS extends Child<TT, U, P, SS>> SS map(Function<? super T, ? extends TT> xform) {
            ensureOpen();
            try {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final SS result = (SS) new ChildImpl(buildStrategy.child(), () -> xform.apply(get()), parent,
                        children.current);
                return result;
            } finally {
                close();
            }
        }

        @Override
        public T get() {
            return buildStrategy.get();
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

        private static class MutatorStrategy<T> implements BuildStrategy<T> {
            final BuildStrategy<T> delegate = prototype();
            final BuildStrategy<?> realParent;

            MutatorStrategy(BuildStrategy<?> realParent) {
                this.realParent = realParent;
            }

            @Override
            public T get() {
                return delegate.get();
            }

            @Override
            public void initialize(Supplier<T> target) {
            }

            @Override
            public void apply(Consumer<? super T> mutation) {
                delegate.apply(mutation);
            }

            @Override
            public <U> BuildStrategy<U> child() {
                return realParent.child();
            }
        }

        P parent;

        MutatorImpl(BuildStrategy<T> buildStrategy, P parent, ChildStrategy childStrategy) {
            super(new MutatorStrategy<>(buildStrategy), null, childStrategy);
            this.parent = parent;
        }

        @Override
        public P onto(Function<? super U, ? extends T> accessor, ComponentStrategy<U, T> strategy) {
            Validate.validState(parent != null);
            parent.then(p -> {
                @SuppressWarnings("unchecked")
                final Function<U, T> x = strategy.apply((Function<U, T>) accessor);
                ((MutatorStrategy<T>) buildStrategy).delegate.initialize(() -> x.apply(p));
                buildStrategy.get();
            });
            try {
                return parent;
            } finally {
                parent = null;
            }
        }
    }

    private static class IntoMapImpl<K, V, U, P extends BlueprintImpl<U, P>, M extends Map<? super K, ? super V>>
            implements IntoMap<K, V, U, P> {

        final Supplier<V> value;
        final Function<? super U, M> map;
        final ChildStrategy childStrategy;
        P parent;

        IntoMapImpl(Supplier<V> value, Function<? super U, M> map, P parent, ChildStrategy childStrategy) {
            this.value = value;
            this.map = map;
            this.parent = parent;
            this.childStrategy = childStrategy;
        }

        @Override
        public P at(Supplier<K> key) {
            Validate.validState(parent != null);

            final BiConsumer<U, V> cmer = childStrategy.apply((u, v) -> map.apply(u).put(key.get(), v));

            parent.then(p -> cmer.accept(p, value.get()));
            try {
                return parent;
            } finally {
                parent = null;
            }
        }
    }

    /**
     * Begin to build a {@link Blueprint} (implicit singleton
     * {@link BuildStrategy}).
     *
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value {@link Supplier}
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R build(Supplier<T> t) {
        return build(singleton(), t);
    }

    /**
     * Begin to build a {@link Blueprint}.
     *
     * @param strategy for build
     * @param t        value {@link Supplier}
     *
     * @param <T>      built type
     * @param <R>      {@link Blueprint.Root} type
     * @return R
     */
    @SuppressWarnings("unchecked")
    public static <T, R extends Blueprint.Root<T, R>> R build(BuildStrategy<T> strategy, Supplier<T> t) {
        return (R) new RootImpl<>(Objects.requireNonNull(strategy), t);
    }

    /**
     * Begin to build a {@link Blueprint} (implicit singleton
     * {@link BuildStrategy}).
     *
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R build(T t) {
        return build(singleton(), () -> t);
    }

    /**
     * Shorthand for {@link #build(Supplier)}.
     *
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value {@link Supplier}
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R $(Supplier<T> t) {
        return build(singleton(), t);
    }

    /**
     * Shorthand for {@link #build(BuildStrategy, Supplier)}.
     *
     * @param strategy for build
     * @param t        value {@link Supplier}
     *
     * @param <T>      built type
     * @param <R>      {@link Blueprint.Root} type
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R $(BuildStrategy<T> strategy, Supplier<T> t) {
        return build(strategy, t);
    }

    /**
     * Shorthand for {@link #build(Object)}.
     *
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R $(T t) {
        return build(singleton(), () -> t);
    }

    /**
     * Begin to build a (root) {@link Collection} blueprint (implicit singleton
     * strategy).
     *
     * @param <E> element type
     * @param <C> built type
     * @param <R> {@link Blueprint.OfCollection} type
     * @param c   value {@link Supplier}
     * @return R
     */
    public static <E, C extends Collection<E>, R extends Blueprint.OfCollection<E, C, R>> R buildCollection(
            Supplier<C> c) {
        return buildCollection(singleton(), c);
    }

    /**
     * Begin to build a (root) {@link Collection} blueprint.
     *
     * @param <E>      element type
     * @param <C>      built type
     * @param <R>      {@link Blueprint.OfCollection} type
     * @param strategy
     * @param c        value {@link Supplier}
     * @return R
     */
    @SuppressWarnings("unchecked")
    public static <E, C extends Collection<E>, R extends Blueprint.OfCollection<E, C, R>> R buildCollection(
            BuildStrategy<C> strategy, Supplier<C> c) {
        return (R) new OfCollectionImpl<>(strategy, c);
    }

    /**
     * Shorthand for {@link #buildCollection(Supplier)}.
     *
     * @param <E> element type
     * @param <C> built type
     * @param <R> {@link Blueprint.OfCollection} type
     * @param c   value {@link Supplier}
     * @return R
     */
    public static <E, C extends Collection<E>, R extends Blueprint.OfCollection<E, C, R>> R c$(Supplier<C> c) {
        return buildCollection(singleton(), c);
    }

    /**
     * Shorthand for {@link #buildCollection(BuildStrategy, Supplier)}.
     *
     * @param <E>      element type
     * @param <C>      built type
     * @param <R>      {@link Blueprint.OfCollection} type
     * @param strategy build strategy
     * @param c        value {@link Supplier}
     * @return R
     */
    public static <E, C extends Collection<E>, R extends Blueprint.OfCollection<E, C, R>> R c$(
            BuildStrategy<C> strategy, Supplier<C> c) {
        return buildCollection(strategy, c);
    }

    /**
     * Begin to build a (root) {@link Map} blueprint.
     *
     * @param <K>      key type
     * @param <V>      value type
     * @param <M>      {@link Map} type
     * @param <R>      {@link Blueprint.OfMap} type
     * @param strategy build strategy
     * @param m        value {@link Supplier}
     * @return R
     */
    @SuppressWarnings("unchecked")
    public static <K, V, M extends Map<K, V>, R extends Blueprint.OfMap<K, V, M, R>> R buildMap(
            BuildStrategy<M> strategy, Supplier<M> m) {
        return (R) new OfMapImpl<>(strategy, m);
    }

    /**
     * Begin to build a (root) {@link Map} blueprint (implicit singleton strategy).
     *
     * @param <K> key type
     * @param <V> value type
     * @param <M> {@link Map} type
     * @param <R> {@link Blueprint.OfMap} type
     * @param m   value {@link Supplier}
     * @return R
     */
    public static <K, V, M extends Map<K, V>, R extends Blueprint.OfMap<K, V, M, R>> R buildMap(Supplier<M> m) {
        return buildMap(singleton(), m);
    }

    /**
     * Shorthand for {@link #buildMap(BuildStrategy, Supplier)}.
     *
     * @param <K>      key type
     * @param <V>      value type
     * @param <M>      {@link Map} type
     * @param <R>      {@link Blueprint.OfMap} type
     * @param strategy build strategy
     * @param m        value {@link Supplier}
     * @return R
     */
    public static <K, V, M extends Map<K, V>, R extends Blueprint.OfMap<K, V, M, R>> R m$(BuildStrategy<M> strategy,
            Supplier<M> m) {
        return buildMap(strategy, m);
    }

    /**
     * Shorthand for {@link #buildMap(Supplier)}.
     *
     * @param <K> key type
     * @param <V> value type
     * @param <M> {@link Map} type
     * @param <R> {@link Blueprint.OfMap} type
     * @param m   value {@link Supplier}
     * @return R
     */
    public static <K, V, M extends Map<K, V>, R extends Blueprint.OfMap<K, V, M, R>> R m$(Supplier<M> m) {
        return buildMap(m);
    }

    private static <T> IntConsumer bindTo(T t, ObjIntConsumer<T> cmer) {
        return i -> cmer.accept(t, i);
    }

    private static void iterate(int times, IntConsumer body) {
        if (times == 0) {
            return;
        }
        Validate.isTrue(times > 0);
        for (int i = 0; i < times; i++) {
            body.accept(i);
        }
    }

    private Cotterpin() {
    }
}
