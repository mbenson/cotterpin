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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Transformable {@link Supplier}.
 *
 * @param <T> supplied type
 */
@FunctionalInterface
public interface TransformableSupplier<T> extends Supplier<T> {

    /**
     * Force a lambda to {@link TransformableSupplier}.
     * 
     * @param <T>
     * @param s
     * @return {@code s}
     */
    public static <T> TransformableSupplier<T> of(TransformableSupplier<T> s) {
        return s;
    }

    /**
     * Transform this {@link TransformableSupplier}.
     * 
     * @param <U>
     * @param xform
     * @return {@link TransformableSupplier}
     */
    default <U> TransformableSupplier<U> map(Function<? super T, ? extends U> xform) {
        return () -> xform.apply(get());
    }
}
