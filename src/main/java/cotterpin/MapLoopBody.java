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
import java.util.function.IntConsumer;

import cotterpin.Blueprint.OfMap;

/**
 * {@link Map} loop body.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <M> {@link Map} type
 * @param <B> self type
 */
@FunctionalInterface
public interface MapLoopBody<K, V, M extends Map<K, V>, B extends OfMap<K, V, M, B>>
        extends OfMapSnapin<K, V, M, B>, IntConsumer {}
