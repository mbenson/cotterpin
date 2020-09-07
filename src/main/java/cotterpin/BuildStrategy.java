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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Build strategy.
 */
public interface BuildStrategy<T> extends Supplier<T> {

    /**
     * Obtain a singleton {@link BuildStrategy}.
     * @param <T>
     * @return {@link BuildStrategy}
     */
    public static <T> BuildStrategy<T> singleton() {
        class SingletonStrategy<TT> implements BuildStrategy<TT> {
            TT target;
            
            @Override
            public void initialize(Supplier<TT> target) {
                this.target = target.get();
            }
            
            @Override
            public void apply(Consumer<? super TT> mutation) {
                mutation.accept(target);
            }
            
            @Override
            public TT get() {
                return target;
            }

            @Override
            public <U> BuildStrategy<U> child() {
                return new SingletonStrategy<>();
            }
        }
        return new SingletonStrategy<>();
    }

    /**
     * Obtain a prototype {@link BuildStrategy}.
     * @param <T>
     * @return {@link BuildStrategy}
     */
    public static <T> BuildStrategy<T> prototype() {
        class PrototypeStrategy<TT> implements BuildStrategy<TT> {
            final List<Consumer<? super TT>> mutations = new ArrayList<>();
            Supplier<TT> target;
            
            @Override
            public void initialize(Supplier<TT> target) {
                this.target = target;
            }
            
            @Override
            public void apply(Consumer<? super TT> mutation) {
                mutations.add(mutation);
            }
            
            @Override
            public TT get() {
                TT t = Optional.ofNullable(target).map(Supplier::get).orElseThrow(IllegalStateException::new);
                mutations.forEach(m -> m.accept(t));
                return t;
            }

            @Override
            public <U> BuildStrategy<U> child() {
                return new PrototypeStrategy<>();
            }
        }
        return new PrototypeStrategy<>();
    }

    /**
     * Initialize the build strategy.
     * @param target {@link Supplier}
     */
    void initialize(Supplier<T> target);

    /**
     * Apply the specified mutation.
     * @param mutation
     */
    void apply(Consumer<? super T> mutation);

    /**
     * Obtain a child {@link BuildStrategy}.
     * @param <U>
     * @return {@link BuildStrategy}
     */
    <U> BuildStrategy<U> child();
}
