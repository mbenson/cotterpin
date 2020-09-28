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

import static cotterpin.BuildStrategy.prototype;
import static cotterpin.BuildStrategy.singleton;
import static cotterpin.ChildStrategy.DEFAULT;
import static cotterpin.ChildStrategy.IGNORE_NULL_PARENT;
import static cotterpin.ChildStrategy.IGNORE_NULL_VALUE;
import static cotterpin.ComponentStrategy.ifNull;
import static cotterpin.Cotterpin.$;
import static cotterpin.Cotterpin.c$;
import static cotterpin.Cotterpin.m$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import acme.Character;
import acme.CharacterType;
import acme.Franchise;
import acme.Franchise.Info;

public class ShorthandTest {

    @Test
    public void testImplicitSingleton() {
        Supplier<Franchise> s = $(Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testExplicitSingleton() {
        Supplier<Franchise> s = $(singleton(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testPrototype() {
        Supplier<Franchise> s = $(prototype(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isNotSameAs(franchise);
    }

    @Test
    public void testSimpleProperty() {
        assertThat(
        // @formatter:off
            $(Franchise::new)
                .$$("Psycho").onto(Franchise::setName)
            .get())
            // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Psycho");
    }

    @Test
    public void testMapProperty() {
        assertThat(
        // @formatter:off
        $(Franchise::new)
            .$$("Nightmare on Elm Street").onto(Franchise::setName)
            .$$(Character::new)
                .$$(CharacterType.GHOST).onto(Character::setType)
            .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Freddy Krueger")
        .get())
        // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Nightmare on Elm Street")
                                .satisfies(f -> assertThat(f.getCharacters()).hasEntrySatisfying("Freddy Krueger",
                                        c -> assertThat(c.getType()).isSameAs(CharacterType.GHOST)));
    }

    @Test
    public void testCollectionProperty() {
        assertThat(
        // @formatter:off
        $(Franchise::new)
            .$$("Evil Dead").onto(Franchise::setName)
            .$$(Character::new)
                .$$(CharacterType.UNDEAD).onto(Character::setType)
                .$$("Book").addTo(Character::getWeaknesses, ifNull(Character::setWeaknesses, LinkedHashSet<String>::new))
                .$$("Chainsaw").addTo(Character::getWeaknesses)
                .$$("Boomstick").addTo(Character::getWeaknesses)
            .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Henrietta")
        .get())
        // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Evil Dead")
                                .satisfies(f -> assertThat(f.getCharacters()).hasEntrySatisfying("Henrietta",
                                        c -> assertThat(c.getWeaknesses()).containsExactly("Book", "Chainsaw",
                                                "Boomstick")));
    }

    @Test
    public void testComponent() {
        assertThat(
        // @formatter:off
        $(Franchise::new)
            .$$("Halloween").onto(Franchise::setName)
            .__(Franchise.Info.class)
                .$$(Year.of(1978)).onto(Franchise.Info::setOriginated)
            .onto(Franchise::getInfo)
        .get())
        // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Halloween").extracting(Franchise::getInfo)
                                .hasFieldOrPropertyWithValue("originated", Year.of(1978));
    }

    @Test
    public void testComponentWithTypeLiteral() {
        assertThat(
        // @formatter:off
        $(Franchise::new)
            .$$("Halloween").onto(Franchise::setName)
            .__(new TypeLiteral<Franchise.Info>() {})
                .$$(Year.of(1978)).onto(Franchise.Info::setOriginated)
            .onto(Franchise::getInfo)
        .get())
        // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Halloween").extracting(Franchise::getInfo)
                                .hasFieldOrPropertyWithValue("originated", Year.of(1978));
    }

    @Test
    public void testLazyComponent() {
        Franchise f = new Franchise();
        Info info = f.getInfo();
        assertThat($(f).__(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new)).get().getInfo())
                .isSameAs(info);
    }

    @Test
    public void testMissingComponent() {
        Franchise f = new Franchise();
        f.setInfo(null);
        assertThat($(f).__(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new)).get().getInfo())
                .isNotNull();
    }

    @Test
    public void testMapRoot() {
        assertThat($(Franchise::new).map(Optional::of).get().get()).isNotNull();
    }

    @Test
    public void testMapChild() {
        assertThat(
        // @formatter:off
        $(Franchise::new)
            .$$("New Line").map(Optional::of).onto(Franchise::maybeSetStudio)
        .get()
        // @formatter:on
                        .getStudio()).isEqualTo("New Line");
    }

    @Test
    public void testNull() {
        Assertions.assertThat($(() -> null).get()).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void testNullRootWithDefaultChildStrategy() {
        $((Franchise) null).$$("").onto(Franchise::setName).get();
    }

    @Test
    public void testIgnoreNullParentStrategy() {
        Assertions.assertThat($((Franchise) null).strategy(IGNORE_NULL_PARENT).child("").onto(Franchise::setName).get())
                .isNull();
    }

    @Test
    public void testIgnoreNullValueStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        $(franchise).strategy(IGNORE_NULL_VALUE).$$((String) null).onto(Franchise::setName).get();
        Mockito.verify(franchise, never()).setName(null);
    }

    @Test
    public void testRestoreDefaultStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        $(franchise).strategy(IGNORE_NULL_VALUE).$$((String) null).strategy(DEFAULT).onto(Franchise::setName).get();
        Mockito.verify(franchise, times(1)).setName(null);
    }

    @Test
    public void testResetStrategy() {
        Assertions.assertThatThrownBy(() -> {
            $((Character) null)
            //@formatter:off
                .strategy(IGNORE_NULL_PARENT)
                .$$(CharacterType.ALIEN).strategy(DEFAULT, IGNORE_NULL_VALUE)
                .onto(Character::setType)
                //@formatter:on
                    .get();
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testRootCollection() {
        assertThat(
        //@formatter:off
            c$(() -> new ArrayList<Character>())
                .$$(Character::new)
                    .$$(CharacterType.ALIEN).onto(Character::setType)
                .add()
                .$$(Character::new)
                    .$$(CharacterType.DEMON).onto(Character::setType)
                .add()
                .$$(Character::new)
                    .$$(CharacterType.GHOST).onto(Character::setType)
                .add()
            .get()
        //@formatter:on
        ).extracting(Character::getType).containsExactly(CharacterType.ALIEN, CharacterType.DEMON, CharacterType.GHOST);
    }

    @Test
    public void testTransformedRootCollection() {
        assertThat(
        //@formatter:off
            c$(() -> new ArrayList<Character>())
                .$$(Character::new)
                    .$$(CharacterType.ALIEN).onto(Character::setType)
                .add()
                .$$(Character::new)
                    .$$(CharacterType.DEMON).onto(Character::setType)
                .add()
                .$$(Character::new)
                    .$$(CharacterType.GHOST).onto(Character::setType)
                .add()
            .map(Collections::unmodifiableList)
            .get()
        //@formatter:on
        ).satisfies(c -> {
            assertThat(c).isInstanceOf(Collections.unmodifiableList(Collections.emptyList()).getClass());
            assertThat(c).extracting(Character::getType).containsExactly(CharacterType.ALIEN, CharacterType.DEMON,
                    CharacterType.GHOST);
        });
    }

    @Test
    public void testRootMap() {
        assertThat(
        //@formatter:off
            m$(() -> new LinkedHashMap<String, Character>())
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Blade")
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Pinhead")
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Jester")
            .get()
        //@formatter:on
        ).satisfies(m -> {
            assertThat(m.keySet()).containsExactly("Blade", "Pinhead", "Jester");
            assertThat(m.values()).extracting(Character::getType).allSatisfy(CharacterType.GOLEM::equals);
        });
    }

    @Test
    public void testTransformedRootMap() {
        assertThat(
        //@formatter:off
            m$(() -> new LinkedHashMap<String, Character>())
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Blade")
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Pinhead")
                .$$(Character::new)
                    .$$(CharacterType.GOLEM).onto(Character::setType)
                .at("Jester")
            .map(Collections::unmodifiableMap)
            .get()
        //@formatter:on
        ).satisfies(m -> {
            assertThat(m).isInstanceOf(Collections.unmodifiableMap(Collections.emptyMap()).getClass());
            assertThat(m.keySet()).containsExactly("Blade", "Pinhead", "Jester");
            assertThat(m.values()).extracting(Character::getType).allSatisfy(CharacterType.GOLEM::equals);
        });
    }

    @Test
    public void testBasicLoop() {
        Franchise f = $(Mockito.mock(Franchise.class)).x(5, (b, i) -> b.$$(String.valueOf(i)).onto(Franchise::setName))
                .get();

        Mockito.verify(f, times(5)).setName(Mockito.anyString());
    }

    @Test
    public void testCollectionLoop() {
        assertThat(
        //@formatter:off
            c$(() -> new TreeSet<Integer>())
                .x(5, (b, i) ->
                    b.$$(Integer.valueOf(i)).add()
                )
            .get()
        //@formatter:on
        ).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void testMapLoop() {
        assertThat(
        // @formatter:off
            m$(() -> new TreeMap<Integer, String>())
                .x(5, (b, i) ->
                    b.$$(String.valueOf(i)).at(i)
                )
            .get()
        // @formatter:on
        ).containsExactly(Pair.of(0, "0"), Pair.of(1, "1"), Pair.of(2, "2"), Pair.of(3, "3"), Pair.of(4, "4"));
    }

    @Test
    public void testNestedLoop() {
        // @formatter:off
        assertThat(
            m$(() -> new TreeMap<Integer, List<Integer>>())
                .x(3, (b, i) ->
                    b.$$(ArrayList::new)
                        .x(3, (l, j) ->
                            l.$$(j).addTo(Function.identity())
                        )
                    .at(i)
                )
            .get()
        // @formatter:on
        ).containsOnlyKeys(0, 1, 2).allSatisfy((k, v) -> assertThat(v).containsExactly(0, 1, 2));
    }
}
