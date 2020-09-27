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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.Typed;

/**
 * Base for {@link Blueprint} {@link FunctionalInterface} extensions.
 *
 * @param <T> built type
 * @param <B> {@link Blueprint} type
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface BlueprintSnapin<T, B extends Blueprint<T, B>> extends Blueprint<T, B> {

    static final ThreadLocal<Blueprint> CURRENT = new ThreadLocal<>();

    static void with(Blueprint snapin, Runnable r) {
        final Blueprint existing = CURRENT.get();
        CURRENT.set(snapin);
        try {
            r.run();
        } finally {
            CURRENT.set(existing);
        }
    }

    static Blueprint current() {
        final Blueprint result = CURRENT.get();
        Validate.validState(result != null);
        return result;
    }

    @Override
    default <X, C extends Child<X, T, B, C>> C child(Supplier<X> c) {
        return (C) current().child(c);
    }

    @Override
    default <X, M extends Mutator<X, T, B, M>> M mutate(Typed<X> type) {
        return (M) current().mutate(type);
    }

    @Override
    default B strategy(ChildStrategy... strategies) {
        return (B) current().strategy(strategies);
    }

    @Override
    default B then(Consumer<? super T> mutation) {
        return (B) current().then(mutation);
    }

    @Override
    default B times(int n, LoopBody<T, B> body) {
        return (B) current().times(n, body);
    }
}
