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

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;

import cotterpin.Blueprint.OfMap;
import cotterpin.Blueprint.OfMapEntry;
import cotterpin.Blueprint.Root;

/**
 * {@link OfMap} snapin interface.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <M> {@link Map} type
 * @param <B> self type
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface OfMapSnapin<K, V, M extends Map<K, V>, B extends OfMap<K, V, M, B>> extends OfMap<K, V, M, B> {

    static final ThreadLocal<OfMap> CURRENT = new ThreadLocal<>();

    static OfMap current() {
        final OfMap result = CURRENT.get();
        Validate.validState(result != null);
        return result;
    }

    static void with(OfMap snapin, Runnable r) {
        final OfMap existing = CURRENT.get();
        CURRENT.set(snapin);
        try {
            r.run();
        } finally {
            CURRENT.set(existing);
        }
    }

    @Override
    default <R extends OfMapEntry<K, V, M, B, R>> R value(Supplier<V> v) {
        return (R) current().value(v);
    }

    @Override
    default B times(int n, MapLoopBody<K, V, M, B> body) {
        return (B) current().times(n, body);
    }

    @Override
    default <T, SS extends Root<T, SS>> SS map(Function<? super M, ? extends T> xform) {
        return (SS) current().map(xform);
    }

    @Override
    default B strategy(ChildStrategy... strategies) {
        return (B) current().strategy(strategies);
    }

    @Override
    default B then(Consumer<? super M> mutation) {
        return (B) current().then(mutation);
    }

    @Override
    default M get() {
        throw new UnsupportedOperationException();
    }
}
