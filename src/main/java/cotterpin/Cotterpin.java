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

        @Override
        public T get() {
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

            BiConsumer<U, T> cmer = children.apply((u, t) -> x.apply(u).add(t));
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
        public P at(K key) {
            Validate.validState(parent != null);

            final BiConsumer<U, V> cmer = childStrategy.apply((u, v) -> map.apply(u).put(key, v));

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
    @SuppressWarnings("unchecked")
    public static <T, R extends Blueprint.Root<T, R>> R $(BuildStrategy<T> strategy, Supplier<T> t) {
        return (R) new RootImpl<>(strategy, t);
    }

    /**
     * Shorthand for {@link #build(Object)}.
     * {@link BuildStrategy}).
     * 
     * @param <T> built type
     * @param <R> {@link Blueprint.Root} type
     * @param t   value
     * @return R
     */
    public static <T, R extends Blueprint.Root<T, R>> R $(T t) {
        return build(singleton(), () -> t);
    }

    private Cotterpin() {
    }
}
