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

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Encapsulates directions for handling of a {@code null} child value.
 *
 * @param <P> parent type
 * @param <T> child type
 */
public class IfNull<P, T> {

    final BiConsumer<? super P, ? super T> record;
    final Supplier<? extends T> create;

    /**
     * Create a new {@link IfNull} instance.
     * 
     * @param record {@link BiConsumer} to record onto a parent the child object
     *               created by {@code create}
     * @param create {@link Supplier} of {@code T}
     */
    IfNull(BiConsumer<? super P, ? super T> record, Supplier<? extends T> create) {
        this.record = Objects.requireNonNull(record, "record");
        this.create = Objects.requireNonNull(create, "create");
    }
}
