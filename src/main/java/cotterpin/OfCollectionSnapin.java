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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;

import cotterpin.Blueprint.OfCollection;
import cotterpin.Blueprint.OfCollectionElement;
import cotterpin.Blueprint.Root;

/**
 * Base class for {@link OfCollection} {@link FunctionalInterface} extensions.
 *
 * @param <E> element type
 * @param <C> {@link Collection} type
 * @param <B> {@link OfCollection} type
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface OfCollectionSnapin<E, C extends Collection<E>, B extends OfCollection<E, C, B>>
        extends OfCollection<E, C, B> {

    static final ThreadLocal<OfCollection> CURRENT = new ThreadLocal<>();

    static OfCollection current() {
        final OfCollection result = CURRENT.get();
        Validate.validState(result != null);
        return result;
    }

    static void with(OfCollection snapin, Runnable r) {
        final OfCollection existing = CURRENT.get();
        CURRENT.set(snapin);
        try {
            r.run();
        } finally {
            CURRENT.set(existing);
        }
    }

    @Override
    default <R extends OfCollectionElement<E, C, B, R>> R element(Supplier<E> e) {
        return (R) current().element(e);
    }

    @Override
    default B times(int n, CollectionLoopBody<E, C, B> body) {
        return (B) current().times(n, body);
    }

    @Override
    default <T, SS extends Root<T, SS>> SS map(Function<? super C, ? extends T> xform) {
        return (SS) current().map(xform);
    }

    @Override
    default B strategy(ChildStrategy... strategies) {
        return (B) current().strategy(strategies);
    }

    @Override
    default B then(Consumer<? super C> mutation) {
        return (B) current().then(mutation);
    }

    @Override
    default C get() {
        throw new UnsupportedOperationException();
    }
}
