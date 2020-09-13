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
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Strategy interface for retrieving "component" objects. These are considered
 * as those that might already exist on the parent, as well as
 * {@link Collection}s and {@link Map}s.
 */
@FunctionalInterface
public interface ComponentStrategy<P, T> extends UnaryOperator<Function<P, T>> {
    /**
     * Create a {@code noop} {@link ComponentStrategy}.
     * 
     * @param <P>
     * @param <T>
     * @return {@link ComponentStrategy}
     */
    public static <P, T> ComponentStrategy<P, T> noop() {
        return s -> s;
    }

    /**
     * Create a {@link ComponentStrategy} to initialize a missing component.
     * 
     * @param <P>    parent type
     * @param <T>    child type
     * @param record {@link BiConsumer} to record onto a parent the child object
     *               created by {@code create}
     * @param create {@link Supplier} of {@code T}
     * @return {@link IfNull}
     */
    public static <P, T> ComponentStrategy<P, T> ifNull(BiConsumer<? super P, ? super T> store, Supplier<T> create) {
        return new IfNull<P, T>(store, create);
    }

    /**
     * Obtain a composite {@link ComponentStrategy}.
     * @param <P>
     * @param <T>
     * @param strategies
     * @return {@link ComponentStrategy}
     */
    public static <P, T> ComponentStrategy<P, T> composite(@SuppressWarnings("unchecked") ComponentStrategy<P, T>... strategies) {
        if (strategies == null || strategies.length == 0) {
            return noop();
        }
        return Stream.of(strategies).reduce(ComponentStrategy::of).get();
    }

    /**
     * Chain {@code s} to {@code this}.
     * 
     * @param s
     * @return {@link ComponentStrategy}
     */
    default ComponentStrategy<P, T> then(ComponentStrategy<P, T> s) {
        return andThen(s)::apply;
    }

    /**
     * Chain {@code this} to {@code s}.
     * 
     * @param s
     * @return
     */
    default ComponentStrategy<P, T> of(ComponentStrategy<P, T> s) {
        return compose(s)::apply;
    }
}
