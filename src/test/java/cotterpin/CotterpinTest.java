package cotterpin;

import static cotterpin.Cotterpin.ifNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.junit.Test;

import acme.Character;
import acme.CharacterType;
import acme.Franchise;

public class CotterpinTest {

    @Test
    public void testBasic() {
        Supplier<Franchise> s = Cotterpin.build(Franchise::new);
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
}
