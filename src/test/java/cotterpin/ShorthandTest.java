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

public class ShorthandTest {

    @Test
    public void testImplicitSingleton() {
        Supplier<Franchise> s = Cotterpin.$(Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testExplicitSingleton() {
        Supplier<Franchise> s = Cotterpin.$(singleton(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isSameAs(franchise);
    }

    @Test
    public void testPrototype() {
        Supplier<Franchise> s = Cotterpin.$(prototype(), Franchise::new);
        Franchise franchise = s.get();
        assertThat(franchise).isNotNull();
        assertThat(s.get()).isNotNull().isNotSameAs(franchise);
    }

    @Test
    public void testSimpleProperty() {
        assertThat(
        // @formatter:off
            Cotterpin.$(Franchise::new)
                .$$("Psycho").onto(Franchise::setName)
            .get())
            // @formatter:on
                                .hasFieldOrPropertyWithValue("name", "Psycho");
    }

    @Test
    public void testMapProperty() {
        assertThat(
        // @formatter:off
        Cotterpin.$(Franchise::new)
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
        Cotterpin.$(Franchise::new)
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
        Cotterpin.$(Franchise::new)
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
        Cotterpin.$(Franchise::new)
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
        assertThat(Cotterpin.$(f).__(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new)).get()
                .getInfo()).isSameAs(info);
    }

    @Test
    public void testMissingComponent() {
        Franchise f = new Franchise();
        f.setInfo(null);
        assertThat(Cotterpin.$(f).__(Info.class).onto(Franchise::getInfo, ifNull(Franchise::setInfo, Info::new)).get()
                .getInfo()).isNotNull();
    }

    @Test
    public void testMapRoot() {
        assertThat(Cotterpin.$(Franchise::new).map(Optional::of).get().get()).isNotNull();
    }

    @Test
    public void testMapChild() {
        assertThat(
        // @formatter:off
        Cotterpin.$(Franchise::new)
            .$$("New Line").map(Optional::of).onto(Franchise::maybeSetStudio)
        .get()
        // @formatter:on
                        .getStudio()).isEqualTo("New Line");
    }

    @Test
    public void testNull() {
        Assertions.assertThat(Cotterpin.$(() -> null).get()).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void testNullRootWithDefaultChildStrategy() {
        Cotterpin.$((Franchise) null).$$("").onto(Franchise::setName).get();
    }

    @Test
    public void testIgnoreNullParentStrategy() {
        Assertions.assertThat(
                Cotterpin.$((Franchise) null).strategy(IGNORE_NULL_PARENT).child("").onto(Franchise::setName).get())
                .isNull();
    }

    @Test
    public void testIgnoreNullValueStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        Cotterpin.$(franchise).strategy(IGNORE_NULL_VALUE).$$((String) null).onto(Franchise::setName).get();
        Mockito.verify(franchise, never()).setName(null);
    }

    @Test
    public void testRestoreDefaultStrategy() {
        Franchise franchise = Mockito.mock(Franchise.class);
        Cotterpin.$(franchise).strategy(IGNORE_NULL_VALUE).$$((String) null).strategy(DEFAULT).onto(Franchise::setName)
                .get();
        Mockito.verify(franchise, times(1)).setName(null);
    }

    @Test
    public void testResetStrategy() {
        Assertions.assertThatThrownBy(() -> {
            Cotterpin.$((Character) null)
            //@formatter:off
                .strategy(IGNORE_NULL_PARENT)
                .$$(CharacterType.ALIEN).strategy(DEFAULT, IGNORE_NULL_VALUE)
                .onto(Character::setType)
                //@formatter:on
                    .get();
        }).isInstanceOf(NullPointerException.class);
    }
}
