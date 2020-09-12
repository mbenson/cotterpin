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
import static cotterpin.Cotterpin.ifNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Year;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.TypeLiteral;
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

    @Test(expected = NullPointerException.class)
    public void testNullRootWithDefaultChildStrategy() {
        Cotterpin.build((Franchise) null).child("").onto(Franchise::setName).get();
    }

    @Test
    public void testIgnoreNullParentStrategy() {
        Assertions.assertThat(Cotterpin.build((Franchise) null).strategy(ChildStrategy.IGNORE_NULL_PARENT).child("")
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
        Assertions.assertThatThrownBy(() -> {
            Cotterpin.build((Character) null)
            //@formatter:off
                .strategy(ChildStrategy.IGNORE_NULL_PARENT)
                .child(CharacterType.ALIEN).strategy(ChildStrategy.DEFAULT, ChildStrategy.IGNORE_NULL_VALUE)
                .onto(Character::setType)
                //@formatter:on
                    .get();
        }).isInstanceOf(NullPointerException.class);
    }
}
