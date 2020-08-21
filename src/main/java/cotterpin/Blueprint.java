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
