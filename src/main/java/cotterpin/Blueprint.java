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

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.Typed;

/**
 * Blueprint for an object graph.
 *
 * @param <T> built type
 * @param <S> self type
 */
public interface Blueprint<T, S extends Blueprint<T, S>> {
    /**
     * Root Blueprint type.
     *
     * @param <T> built type
     * @param <S> self type
     */
    public interface Root<T, S extends Root<T, S>> extends Blueprint<T, S>, Supplier<T> {

        /**
         * Transform this {@link Blueprint}.
         * 
         * @param <TT>  new built type
         * @param <SS>  new self type
         * @param xform {@link Function}
         * @return {@code SS}
         */
        <TT, SS extends Root<TT, SS>> SS map(Function<? super T, ? extends TT> xform);
    }

    /**
     * Blueprint of {@link Collection}.
     *
     * @param <E> element type
     * @param <C> {@link Collection} type
     * @param <S> self type
     */
    public interface OfCollection<E, C extends Collection<E>, S extends OfCollection<E, C, S>> extends Supplier<C> {

        /**
         * Obtain a blueprint for a directly-specified element.
         * 
         * @param <R> {@link OfCollectionElement} type
         * @param e   element
         * @return R
         */
        default <R extends OfCollectionElement<E, C, S, R>> R element(E e) {
            return element(() -> e);
        }

        /**
         * Obtain a blueprint for a {@link Collection} element.
         * 
         * @param <R> {@link OfCollectionElement} type
         * @param e   element {@link Supplier}
         * @return R
         */
        <R extends OfCollectionElement<E, C, S, R>> R element(Supplier<E> e);

        /**
         * Shorthand for {@link #element(Supplier)}.
         * 
         * @param <R> {@link OfCollectionElement} type
         * @param e   element {@link Supplier}
         * @return R
         */
        default <R extends OfCollectionElement<E, C, S, R>> R $$(Supplier<E> e) {
            return element(e);
        }

        /**
         * Shorthand for {@link #element(Object)}
         * 
         * @param <R> {@link OfCollectionElement} type
         * @param e   element
         * @return R
         */
        default <R extends OfCollectionElement<E, C, S, R>> R $$(E e) {
            return element(() -> e);
        }

        /**
         * Shorthand syntax for {@code null} {@link #element(Object)}.
         * 
         * @param <R>
         * @return R
         */
        default <R extends OfCollectionElement<E, C, S, R>> R nul() {
            return element(() -> null);
        }

        /**
         * Add a step to the blueprint plan.
         * 
         * @param mutation which will be applied
         * @return {@code this}, fluently
         */
        S then(Consumer<? super C> mutation);

        /**
         * In conjunction with inherited strategies (where applicable), apply the
         * specified {@link ChildStrategy} set from this point onward until and unless
         * this method is called again.
         * 
         * @param strategies
         * @return {@code this}, fluently
         */
        S strategy(ChildStrategy... strategies);
    }

    /**
     * Blueprint of {@link Collection} element.
     *
     * @param <E> element type
     * @param <C> {@link Collection} type
     * @param <P> parent type
     * @param <S> self type
     */
    public interface OfCollectionElement<E, C extends Collection<E>, P extends OfCollection<E, C, P>, S extends OfCollectionElement<E, C, P, S>>
            extends Blueprint<E, S> {

        /**
         * Add the built element to the hosted {@link Collection}
         * 
         * @return parent blueprint, fluently
         */
        P add();
    }

    /**
     * Blueprint of {@link Map}.
     *
     * @param <K> key type
     * @param <V> value type
     * @param <M> {@link Map} type
     * @param <S> self type
     */
    public interface OfMap<K, V, M extends Map<K, V>, S extends OfMap<K, V, M, S>> extends Supplier<M> {

        /**
         * Obtain a blueprint for a {@link Map} entry (via its value).
         * 
         * @param <R> {@link OfMapEntry} type
         * @param v   value {@link Supplier}
         * @return R
         */
        <R extends OfMapEntry<K, V, M, S, R>> R value(Supplier<V> v);

        /**
         * Obtain a blueprint for a {@link Map} entry (via directly-specified value).
         * 
         * @param <R>
         * @param v   value
         * @return R
         */
        <R extends OfMapEntry<K, V, M, S, R>> R value(V v);

        /**
         * Shorthand for {@link #value(Supplier)}.
         * 
         * @param <R>
         * @param v   value {@link Supplier}
         * @return R
         */
        default <R extends OfMapEntry<K, V, M, S, R>> R $$(Supplier<V> v) {
            return value(v);
        }

        /**
         * Shorthand for {@link #value(Object)}.
         * 
         * @param <R>
         * @param v   value
         * @return R
         */
        default <R extends OfMapEntry<K, V, M, S, R>> R $$(V v) {
            return value(() -> v);
        }

        /**
         * Shorthand syntax for {@code null} {@link #value(Object)}.
         * 
         * @param <R>
         * @return R
         */
        <R extends OfMapEntry<K, V, M, S, R>> R nul();

        /**
         * Add a step to the blueprint plan.
         * 
         * @param mutation which will be applied
         * @return {@code this}, fluently
         */
        S then(Consumer<? super M> mutation);

        /**
         * In conjunction with inherited strategies (where applicable), apply the
         * specified {@link ChildStrategy} set from this point onward until and unless
         * this method is called again.
         * 
         * @param strategies
         * @return {@code this}, fluently
         */
        S strategy(ChildStrategy... strategies);
    }

    /**
     * Blueprint of {@link Map} entry.
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param <P>
     * @param <S>
     */
    public interface OfMapEntry<K, V, M extends Map<K, V>, P extends OfMap<K, V, M, P>, S extends OfMapEntry<K, V, M, P, S>>
            extends Blueprint<V, S> {

        /**
         * Put the built value into the hosted {@link Map} at {@code key}.
         * 
         * @param key
         * @return parent blueprint, fluently
         */
        P at(K key);
    }

    /**
     * Blueprint for an object subordinate to the graph root.
     *
     * @param <T> built type
     * @param <U> parent type
     * @param <P> parent blueprint type
     * @param <S> self type
     */
    public interface Child<T, U, P extends Blueprint<U, P>, S extends Child<T, U, P, S>> extends Blueprint<T, S> {

        /**
         * Apply the supplied value to the parent using the specified {@code mutator}.
         * 
         * @param mutator
         * @return parent blueprint, fluently
         */
        P onto(BiConsumer<? super U, ? super T> mutator);

        /**
         * Add the supplied value to a {@link Collection} property of the parent,
         * obtained by {@code coll}.
         * 
         * @param <C>  {@link Collection} type
         * @param coll {@link Function}
         * @return parent blueprint, fluently
         */
        default <C extends Collection<? super T>> P addTo(Function<? super U, C> coll) {
            return addTo(coll, ComponentStrategy.noop());
        }

        /**
         * Add the supplied value to a {@link Collection} property of the parent,
         * obtained by {@code coll}, with component strategy.
         * 
         * @param <C>      {@link Collection} type
         * @param coll     {@link Function}
         * @param strategy to apply
         * @return parent blueprint, fluently
         */
        <C extends Collection<? super T>> P addTo(Function<? super U, C> coll, ComponentStrategy<U, C> strategy);

        /**
         * Begin the process of putting the supplied value into a {@link Map} property
         * of the parent, obtained by {@code map}.
         * 
         * @param <K> key type
         * @param <M> {@link Map} type
         * @param map {@link Function}
         * @return {@link IntoMap} fluent step
         */
        default <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map) {
            return into(map, ComponentStrategy.noop());
        }

        /**
         * Begin the process of putting the supplied value into a {@link Map} property
         * of the parent, obtained by {@code map}, with component strategy.
         * 
         * @param <K>      key type
         * @param <M>      {@link Map} type
         * @param map      {@link Function}
         * @param strategy to apply
         * @return {@link IntoMap} fluent step
         */
        <K, M extends Map<? super K, ? super T>> IntoMap<K, T, U, P> into(Function<? super U, M> map,
                ComponentStrategy<U, M> strategy);

        /**
         * Transform this {@link Child}.
         * 
         * @param <TT>  new built type
         * @param <SS>  new self type
         * @param xform {@link Function}
         * @return {@code SS}
         */
        <TT, SS extends Child<TT, U, P, SS>> SS map(Function<? super T, ? extends TT> xform);
    }

    /**
     * Fluent step for map insertion.
     *
     * @param <K> key type
     * @param <V> value type
     * @param <U> parent type
     * @param <P> parent blueprint type
     */
    public interface IntoMap<K, V, U, P extends Blueprint<U, P>> {

        /**
         * Complete the ongoing map insertion using the specified {@code key}.
         * 
         * @param key
         * @return parent blueprint
         */
        P at(K key);
    }

    /**
     * Blueprint for a child "component" which may be an object that already exists
     * on the parent.
     *
     * @param <T> component type
     * @param <U> parent type
     * @param <P> parent blueprint type
     * @param <S> self type
     */
    public interface Mutator<T, U, P extends Blueprint<U, P>, S extends Mutator<T, U, P, S>> extends Blueprint<T, S> {

        /**
         * Apply the configured mutations onto the component obtained using
         * {@code accessor}.
         * 
         * @param accessor {@link Function}
         * @return parent blueprint
         */
        default P onto(Function<? super U, ? extends T> accessor) {
            return onto(accessor, ComponentStrategy.noop());
        }

        /**
         * Apply the configured mutations onto the component obtained using
         * {@code accessor}, with component strategy.
         * 
         * @param accessor {@link Function}
         * @param strategy to apply
         * @return parent blueprint
         */
        P onto(Function<? super U, ? extends T> accessor, ComponentStrategy<U, T> strategy);
    }

    /**
     * Obtain a blueprint for a child value.
     * 
     * @param <X> value type
     * @param <C> {@link Child} blueprint type
     * @param c   child {@link Supplier}
     * @return C
     */
    <X, C extends Child<X, T, S, C>> C child(Supplier<X> c);

    /**
     * Obtain a blueprint for a directly-specified child value.
     * 
     * @param <X> value type
     * @param <C> {@link Child} blueprint type
     * @param c   child value
     * @return C
     */
    default <X, C extends Child<X, T, S, C>> C child(X c) {
        return child(() -> c);
    }

    /**
     * Shorthand for {@link #child(Supplier)}.
     * 
     * @param <X> value type
     * @param <C> {@link Child} blueprint type
     * @param c   child {@link Supplier}
     * @return C
     */
    default <X, C extends Child<X, T, S, C>> C $$(Supplier<X> c) {
        return child(c);
    }

    /**
     * Shorthand for {@link #child(Object)}.
     * 
     * @param <X> value type
     * @param <C> {@link Child} blueprint type
     * @param c   child value
     * @return C
     */
    default <X, C extends Child<X, T, S, C>> C $$(X c) {
        return child(() -> c);
    }

    /**
     * Obtain a child blueprint with a {@code null} value, avoiding casts.
     * 
     * @param <X>  value type
     * @param <C>  {@link Child} blueprint type
     * @param type
     * @return C
     */
    default <X, C extends Child<X, T, S, C>> C nul(Typed<X> type) {
        return child(() -> null);
    }

    /**
     * Obtain a child blueprint with a {@code null} value, avoiding casts.
     * 
     * @param <X>  value type
     * @param <C>  {@link Child} blueprint type
     * @param type
     * @return C
     */
    default <X, C extends Child<X, T, S, C>> C nul(Class<X> type) {
        return child(() -> null);
    }

    /**
     * Obtain a blueprint for a child component of the specified {@code type}.
     * 
     * @param <X>  component type
     * @param <M>  fully parameterized {@link Mutator} type
     * @param type
     * @return M
     */
    <X, M extends Mutator<X, T, S, M>> M mutate(Typed<X> type);

    /**
     * Obtain a blueprint for a child component of the specified {@code type}.
     * 
     * @param <X>  component type
     * @param <M>  fully parameterized {@link Mutator} type
     * @param type
     * @return M
     */
    <X, M extends Mutator<X, T, S, M>> M mutate(Class<X> type);

    /**
     * Shorthand for {@link #mutate(Typed)}.
     * 
     * @param <X>  component type
     * @param <M>  fully parameterized {@link Mutator} type
     * @param type
     * @return M
     */
    default <X, M extends Mutator<X, T, S, M>> M __(Typed<X> type) {
        return mutate(type);
    }

    /**
     * Shorthand for {@link #mutate(Class)}.
     * 
     * @param <X>  component type
     * @param <M>  fully parameterized {@link Mutator} type
     * @param type
     * @return M
     */
    default <X, M extends Mutator<X, T, S, M>> M __(Class<X> type) {
        return mutate(type);
    }

    /**
     * Add a step to the blueprint plan.
     * 
     * @param mutation which will be applied
     * @return {@code this}, fluently
     */
    S then(Consumer<? super T> mutation);

    /**
     * In conjunction with inherited strategies (where applicable), apply the
     * specified {@link ChildStrategy} set from this point onward until and unless
     * this method is called again.
     * 
     * @param strategies
     * @return {@code this}, fluently
     */
    S strategy(ChildStrategy... strategies);
}
