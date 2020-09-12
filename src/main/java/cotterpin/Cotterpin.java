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

    private static class BlueprintImpl<T, S extends BlueprintImpl<T, S>> implements Blueprint<T, S> {

        final BuildStrategy<T> buildStrategy;
        final ChildStrategyManager children;

        BlueprintImpl(BuildStrategy<T> buildStrategy, Supplier<T> target, ChildStrategy childStrategy) {
            this.buildStrategy = buildStrategy;
            buildStrategy.initialize(target);
            children = new ChildStrategyManager(childStrategy);
        }

        @Override
        public T get() {
            return buildStrategy.get();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, C extends Child<X, T, S, C>> C child(Supplier<X> c) {
            return (C) new ChildImpl(buildStrategy.child(), Objects.requireNonNull(c), this, children.current);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, M extends Mutator<X, T, S, M>> M mutate(Typed<X> type) {
            return (M) new MutatorImpl(buildStrategy.child(), this, children.current);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <X, M extends Mutator<X, T, S, M>> M mutate(Class<X> type) {
            return (M) new MutatorImpl(buildStrategy.child(), this, children.current);
        }

        @Override
        @SuppressWarnings("unchecked")
        public S then(Consumer<? super T> mutation) {
            buildStrategy.apply(mutation);
            return (S) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public S strategy(ChildStrategy... strategies) {
            children.adopt(strategies);
            return (S) this;
        }
    }

    private static class RootImpl<T, S extends RootImpl<T, S>> extends BlueprintImpl<T, S>
            implements Blueprint.Root<T, S> {

        RootImpl(BuildStrategy<T> buildStrategy, Supplier<T> target) {
            super(buildStrategy, target, ChildStrategy.DEFAULT);
        }

        @Override
        public <TT, SS extends Root<TT, SS>> SS map(Function<? super T, ? extends TT> xform) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final SS result = (SS) new RootImpl(buildStrategy.child(),
                    () -> Objects.requireNonNull(xform).apply(get()));
            return result;
        }
    }

    private static class ChildImpl<T, U, P extends BlueprintImpl<U, P>, S extends ChildImpl<T, U, P, S>>
            extends BlueprintImpl<T, S> implements Child<T, U, P, S> {

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
        public <C extends Collection<? super T>> P addTo(Function<? super U, C> coll) {
            ensureOpen();
            BiConsumer<U, T> cmer = children.apply((u, t) -> coll.apply(u).add(t));
            parent.then(p -> cmer.accept(p, get()));
            return close();
        }

        @Override
        public <C extends Collection<? super T>> P addTo(Function<? super U, C> coll, IfNull<U, C> ifNull) {
            ensureOpen();
            BiConsumer<U, T> cmer = children.apply((u, t) -> obtainFrom(u, coll, ifNull).add(t));
            parent.then(p -> cmer.accept(p, get()));
            return close();
        }

        @Override
        public <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map,
                IfNull<U, M> ifNull) {
            ensureOpen();
            try {
                return new IntoMapImpl<>(this, map, parent, ifNull, children.current);
            } finally {
                parent = null;
            }
        }

        @Override
        public <TT, SS extends Child<TT, U, P, SS>> SS map(Function<? super T, ? extends TT> xform) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final SS result = (SS) new ChildImpl(buildStrategy.child(), () -> xform.apply(get()), parent,
                    children.current);
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
        public P onto(Function<? super U, ? extends T> prop, IfNull<U, T> ifNull) {
            parent.then(p -> {
                ((MutatorStrategy<T>) buildStrategy).delegate.initialize(() -> obtainFrom(p, prop, ifNull));
                buildStrategy.get();
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

        final Supplier<V> value;
        final Function<? super U, M> map;
        final IfNull<U, M> ifNull;
        final ChildStrategy childStrategy;
        P parent;

        IntoMapImpl(Supplier<V> value, Function<? super U, M> map, P parent, IfNull<U, M> ifNull,
                ChildStrategy childStrategy) {
            this.value = value;
            this.map = map;
            this.parent = parent;
            this.ifNull = ifNull;
            this.childStrategy = childStrategy;
        }

        @Override
        public P at(K key) {
            Validate.validState(parent != null);

            BiConsumer<U, V> cmer = childStrategy.apply((u, v) -> obtainFrom(u, map, ifNull).put(key, v));

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
        return (R) new RootImpl<>(strategy, t);
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
