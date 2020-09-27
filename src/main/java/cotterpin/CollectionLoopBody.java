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
import java.util.function.IntConsumer;

import cotterpin.Blueprint.OfCollection;

/**
 * {@link Collection} loop body.
 *
 * @param <E> element type
 * @param <C> {@link Collection} type
 * @param <B> {@link OfCollection} type
 */
@FunctionalInterface
public interface CollectionLoopBody<E, C extends Collection<E>, B extends OfCollection<E, C, B>>
        extends OfCollectionSnapin<E, C, B>, IntConsumer {}