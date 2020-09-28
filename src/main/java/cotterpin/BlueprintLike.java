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
     * Repeat the specified loop body {@code times} times.
     * 
     * @param times
     * @param body
     * @return {@code this}, fluently
     */
    S times(int times, ObjIntConsumer<S> body);

    /**
     * Shorthand for {@link #times(int, LoopBody)}.
     * 
     * @param times
     * @param body
     * @return {@code this}, fluently
     */
    default S x(int times, ObjIntConsumer<S> body) {
        return times(times, body);
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
