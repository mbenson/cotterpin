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
import static cotterpin.ComponentStrategy.ifNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

public class CotterpinTest {

    @Test
    public void testImplicitSingleton() {
        Supplier<Franchise> s = Cotterpin.build(Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testExplicitSingleton() {
        Supplier<Franchise> s = Cotterpin.build(singleton(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testPrototype() {
        Supplier<Franchise> s = Cotterpin.build(prototype(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isNotSameAs(franchise);
    }

    @Test
    public void testSimpleProperty() {
        assertThat(
        // @formatter:off
            Cotterpin.build(Franchise::new)
                .child("Psycho").onto(Franchise::setName)
            .get())
            // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Psycho");
    }

    @Test
    public void testMapProperty() {
        assertThat(
        // @formatter:off
        Cotterpin.build(Franchise::new)
            .child("Nightmare on Elm Street").onto(Franchise::setName)
            .child(Character::new)
                .child(CharacterType.GHOST).onto(Character::setType)
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
        Cotterpin.build(Franchise::new)
            .child("Evil Dead").onto(Franchise::setName)
            .child(Character::new)
                .child(CharacterType.UNDEAD).onto(Character::setType)
                .child("Book").addTo(Character::getWeaknesses, ifNull(Character::setWeaknesses, LinkedHashSet<String>::new))
                .child("Chainsaw").addTo(Character::getWeaknesses)
                .child("Boomstick").addTo(Character::getWeaknesses)
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
        Cotterpin.build(Franchise::new)
            .child("Halloween").onto(Franchise::setName)
            .mutate(Franchise.Info.class)
                .child(Year.of(1978)).onto(Franchise.Info::setOriginated)
            .onto(Franchise::getInfo)
        .get())
                                .hasFieldOrPropertyWithValue("name", "Halloween")
                                .extracting(Franchise::getInfo).hasFieldOrPropertyWithValue("originated", Year.of(1978));
        // @formatter:on
    }

    @Test
    public void testComponentWithTypeLiteral() {
        assertThat(
        // @formatter:off
        Cotterpin.build(Franchise::new)
            .child("Halloween").onto(Franchise::setName)
            .mutate(new TypeLiteral<Franchise.Info>() {})
                .child(Year.of(1978)).onto(Franchise.Info::setOriginated)
            .onto(Franchise::getInfo)
        .get())
                                .hasFieldOrPropertyWithValue("name", "Halloween")
                                .extracting(Franchise::getInfo).hasFieldOrPropertyWithValue("originated", Year.of(1978));
        // @formatter:on
    }

    @Test
    public void testLazyComponent() {
        Franchise f = new Franchise();
        Info info = f.getInfo();
        assertThat(Cotterpin.build(f).mutate(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new))
                .get().getInfo()).isSameAs(info);
    }

    @Test
    public void testMissingComponent() {
        Franchise f = new Franchise();
        f.setInfo(null);
        assertThat(Cotterpin.build(f).mutate(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new))
                .get().getInfo()).isNotNull();
    }

    @Test
    public void testMapRoot() {
        assertThat(Cotterpin.build(Franchise::new).map(Optional::of).get().get()).isNotNull();
    }

    @Test
    public void testMapChild() {
        assertThat(
        // @formatter:off
        Cotterpin.build(Franchise::new)
            .child("New Line").map(Optional::of).onto(Franchise::maybeSetStudio)
        .get()
        // @formatter:on
                        .getStudio()).isEqualTo("New Line");
    }

    @Test
    public void testNull() {
        Assertions.assertThat(Cotterpin.build(() -> null).get()).isNull();
    }

    @Test
    public void testNullChild() {
        assertThat(
        // @formatter:off
        Cotterpin.build(Franchise::new)
            .nul(String.class).onto(Franchise::setName)
        .get()
        // @formatter:on
                        .getName()).isNull();
    }

    @Test
    public void testParameterizedNullChild() {
        assertThat(
        // @formatter:off
        Cotterpin.build(Franchise::new)
            .nul(new TypeLiteral<Map<String, Character>>() {}).onto(Franchise::setCharacters)
        .get().getCharacters()
        // @formatter:on
        ).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void testNullRootWithDefaultChildStrategy() {
        Cotterpin.build((Franchise) null).child("").onto(Franchise::setName).get();
    }

    @Test
    public void testIgnoreNullParentStrategy() {
        assertThat(Cotterpin.build((Franchise) null).strategy(ChildStrategy.IGNORE_NULL_PARENT).child("")
                .onto(Franchise::setName).get()).isNull();
    }

    @Test
    public void testIgnoreNullValueStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        Cotterpin.build(franchise).strategy(ChildStrategy.IGNORE_NULL_VALUE).child((String) null)
                .onto(Franchise::setName).get();
        Mockito.verify(franchise, never()).setName(null);
    }

    @Test
    public void testRestoreDefaultStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        Cotterpin.build(franchise).strategy(ChildStrategy.IGNORE_NULL_VALUE).child((String) null)
                .strategy(ChildStrategy.DEFAULT).onto(Franchise::setName).get();
        Mockito.verify(franchise, times(1)).setName(null);
    }

    @Test
    public void testResetStrategy() {
        assertThatThrownBy(() -> {
            Cotterpin.build((Character) null)
            //@formatter:off
                .strategy(ChildStrategy.IGNORE_NULL_PARENT)
                .child(CharacterType.ALIEN).strategy(ChildStrategy.DEFAULT, ChildStrategy.IGNORE_NULL_VALUE)
                .onto(Character::setType)
                //@formatter:on
                    .get();
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testRootCollection() {
        assertThat(
        //@formatter:off
            Cotterpin.buildCollection(() -> new ArrayList<Character>())
                .element(Character::new)
                    .child(CharacterType.ALIEN).onto(Character::setType)
                .add()
                .element(Character::new)
                    .child(CharacterType.DEMON).onto(Character::setType)
                .add()
                .element(Character::new)
                    .child(CharacterType.GHOST).onto(Character::setType)
                .add()
            .get()
        //@formatter:on
        ).extracting(Character::getType).containsExactly(CharacterType.ALIEN, CharacterType.DEMON, CharacterType.GHOST);
    }

    @Test
    public void testTransformedRootCollection() {
        assertThat(
        //@formatter:off
            Cotterpin.buildCollection(() -> new ArrayList<Character>())
                .element(Character::new)
                    .child(CharacterType.ALIEN).onto(Character::setType)
                .add()
                .element(Character::new)
                    .child(CharacterType.DEMON).onto(Character::setType)
                .add()
                .element(Character::new)
                    .child(CharacterType.GHOST).onto(Character::setType)
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
            Cotterpin.buildMap(() -> new LinkedHashMap<String, Character>())
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
                .at("Blade")
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
                .at("Pinhead")
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
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
            Cotterpin.buildMap(() -> new LinkedHashMap<String, Character>())
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
                .at("Blade")
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
                .at("Pinhead")
                .value(Character::new)
                    .child(CharacterType.GOLEM).onto(Character::setType)
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
        Franchise f = Cotterpin.build(Mockito.mock(Franchise.class))
                .times(5, (b, i) -> b.child(String.valueOf(i)).onto(Franchise::setName)).get();

        Mockito.verify(f, times(5)).setName(Mockito.anyString());
    }

    @Test
    public void testCollectionLoop() {
        assertThat(
        //@formatter:off
            Cotterpin.buildCollection(() -> new TreeSet<Integer>())
                .times(5, (b, i) ->
                    b.element(Integer.valueOf(i)).add()
                )
            .get()
        //@formatter:on
        ).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void testMapLoop() {
        assertThat(
        // @formatter:off
            Cotterpin.buildMap(() -> new TreeMap<Integer, String>())
                .times(5, (b, i) ->
                    b.value(String.valueOf(i)).at(i)
                )
            .get()
        // @formatter:on
        ).containsExactly(Pair.of(0, "0"), Pair.of(1, "1"), Pair.of(2, "2"), Pair.of(3, "3"), Pair.of(4, "4"));
    }

    @Test
    public void testNestedLoop() {
        assertThat(
        // @formatter:off
            Cotterpin.buildMap(() -> new TreeMap<Integer, List<Integer>>())
                .times(3, (b, i) ->
                    b.value(ArrayList::new)
                        .times(3, (l, j) ->
                            l.child(j).addTo(Function.identity())
                        )
                    .at(i)
                )
            .get()
        // @formatter:on
        ).containsOnlyKeys(0, 1, 2).allSatisfy((k, v) -> assertThat(v).containsExactly(0, 1, 2));
    }

    @Test
    public void testBasicForEach() {
        assertThat(
        // @formatter:off
            Cotterpin.build(Franchise::new)
            .each(CharacterType.values()).apply((t, p) ->
                p.child(Character::new)
                    .child(t).onto(Character::setType)
                .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at(t::name)
            ).get().getCharacters()
        // @formatter:on
        ).hasSize(CharacterType.values().length).satisfies(m -> {
            for (CharacterType t : CharacterType.values()) {
                assertThat(m).hasEntrySatisfying(t.name(), c -> assertThat(c.getType()).isSameAs(t));
            }
        });
    }

    @Test
    public void testCollectionForEach() {
        assertThat(
        // @formatter:off
            Cotterpin.buildCollection(() -> new ArrayList<Character>())
            .each(CharacterType.values()).apply((t, p) ->
                p.element(Character::new)
                    .child(t).onto(Character::setType)
                .add()
            ).get()
        // @formatter:on
        ).extracting(Character::getType).containsExactly(CharacterType.values());
    }

    @Test
    public void testMapForEach() {
        assertThat(
        // @formatter:off
            Cotterpin.buildMap(() -> new EnumMap<CharacterType,Character>(CharacterType.class))
            .each(CharacterType.values()).apply((t, p) ->
                p.value(Character::new)
                    .child(t).onto(Character::setType)
                .at(t)
            ).get()
        // @formatter:on
        ).hasSize(CharacterType.values().length).satisfies(m -> {
            for (CharacterType t : CharacterType.values()) {
                assertThat(m).hasEntrySatisfying(t, c -> assertThat(c.getType()).isSameAs(t));
            }
        });
    }
}
