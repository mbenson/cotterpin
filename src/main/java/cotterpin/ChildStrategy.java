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

import java.util.function.BiConsumer;

/**
 * Strategy interface for handling non-root objects in a graph.
 *
 * @param <P>
 * @param <T>
 */
public interface ChildStrategy {

    /**
     * Default/{@code noop} {@link ChildStrategy}. Explicitly specifying this
     * strategy will turn off any contextually-inherited {@link ChildStrategy}.
     */
    public static final ChildStrategy DEFAULT = new ChildStrategy() {

        @Override
        public <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer) {
            return cmer;
        }

        @Override
        public ChildStrategy then(ChildStrategy s) {
            return this;
        }
    };

    /**
     * {@link ChildStrategy} to ignore {@code null} parent objects.
     */
    public static final ChildStrategy IGNORE_NULL_PARENT = new ChildStrategy() {

        @Override
        public <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer) {
            return (p, t) -> {
                if (p != null) {
                    cmer.accept(p, t);
                }
            };
        }
    };

    /**
     * {@link ChildStrategy} to ignore {@code null} value objects.
     */
    public static final ChildStrategy IGNORE_NULL_VALUE = new ChildStrategy() {

        @Override
        public <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer) {
            return (p, t) -> {
                if (t != null) {
                    cmer.accept(p, t);
                }
            };
        }
    };

    /**
     * Apply this strategy to {@code cmer}.
     * 
     * @param <P>
     * @param <T>
     * @param cmer
     * @return {@link BiConsumer}
     */
    <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer);

    /**
     * Chain {@code s} to th{@code this}.
     * 
     * @param s
     * @return {@link ChildStrategy}
     */
    default ChildStrategy then(ChildStrategy s) {
        return new ChildStrategy() {

            @Override
            public <P, T> BiConsumer<P, T> apply(BiConsumer<P, T> cmer) {
                return s.apply(ChildStrategy.this.apply(cmer));
            }
        };
    }
}