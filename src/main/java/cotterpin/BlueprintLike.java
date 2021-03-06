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

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

/**
 * Super-interface for blueprint-like types.
 * 
 * @param <T> built type
 * @param <S> self type
 */
public interface BlueprintLike<T, S extends BlueprintLike<T, S>> {

    /**
     * Step in fluent interface.
     *
     * @param <E> value type
     * @param <B> originating {@link BlueprintLike}
     */
    public interface ForEach<E, B extends BlueprintLike<?, B>> {

        /**
         * Apply to {@code body} each value and the originating blueprint.
         * 
         * @param body
         * @return B
         */
        B apply(BiConsumer<E, B> body);
    }

    /**
     * Repeat the specified loop body {@code times} times.
     * 
     * @param times
     * @param body
     * @return {@code this}, fluently
     */
    S times(int times, ObjIntConsumer<S> body);

    /**
     * Shorthand for {@link #times(int, ObjIntConsumer)}.
     * 
     * @param times
     * @param body
     * @return {@code this}, fluently
     */
    default S x(int times, ObjIntConsumer<S> body) {
        return times(times, body);
    }

    /**
     * Begin the process of iterating over a number of input values.
     * 
     * @param <X>    value type
     * @param values
     * @return {@link ForEach}
     */
    <X> ForEach<X, S> each(Iterable<X> values);

    /**
     * Begin the process of iterating over a number of input values.
     * 
     * @param <X>    value type
     * @param values
     * @return {@link ForEach}
     */
    @SuppressWarnings("unchecked")
    default <X> ForEach<X, S> each(X... values) {
        return each(Arrays.asList(values));
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
