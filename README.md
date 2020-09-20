# cotterpin
Functional Fluent Java Object Graph Builder

Building a Java object graph, particularly of Java beans, is annoying and verbose. Fluent builders are better, but require the creation or generation of custom code for the purpose. Cotterpin provides a fluent interface to emulate a similar experience using Java 8 functors.

### simple property
```
Cotterpin.build(Franchise::new)
    .child("Psycho").onto(Franchise::setName)
.get()
```

Shorthand version (using static import):
```
$(Franchise::new)
    .$$("Psycho").onto(Franchise::setName)
.get()
```

### collection element
```
Cotterpin.build(Franchise::new)
    .child("Evil Dead").onto(Franchise::setName)
    .child(Character::new)
        .child(CharacterType.UNDEAD).onto(Character::setType)
        .child("Book").addTo(Character::getWeaknesses, ifNull(Character::setWeaknesses, LinkedHashSet<String>::new))
        .child("Chainsaw").addTo(Character::getWeaknesses)
        .child("Boomstick").addTo(Character::getWeaknesses)
    .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Henrietta")
.get()
```

Shorthand version:
```
$(Franchise::new)
    .$$("Evil Dead").onto(Franchise::setName)
    .$$(Character::new)
        .$$(CharacterType.UNDEAD).onto(Character::setType)
        .$$("Book").addTo(Character::getWeaknesses, ifNull(Character::setWeaknesses, LinkedHashSet<String>::new))
        .$$("Chainsaw").addTo(Character::getWeaknesses)
        .$$("Boomstick").addTo(Character::getWeaknesses)
    .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Henrietta")
.get()
```

### map entry
```
Cotterpin.build(Franchise::new)
    .child("Nightmare on Elm Street").onto(Franchise::setName)
    .child(Character::new)
        .child(CharacterType.GHOST).onto(Character::setType)
    .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Freddy Krueger")
.get()
```

Shorthand version:
```
$(Franchise::new)
    .$$("Nightmare on Elm Street").onto(Franchise::setName)
    .$$(Character::new)
        .$$(CharacterType.GHOST).onto(Character::setType)
    .into(Franchise::getCharacters, ifNull(Franchise::setCharacters, TreeMap::new)).at("Freddy Krueger")
.get()
```

### raw typed component
```
Cotterpin.build(Franchise::new)
    .child("Halloween").onto(Franchise::setName)
    .mutate(Franchise.Info.class)
        .child(Year.of(1978)).onto(Franchise.Info::setOriginated)
    .onto(Franchise::getInfo)
.get()
```

Shorthand version:
```
$(Franchise::new)
    .$$("Halloween").onto(Franchise::setName)
    .__(Franchise.Info.class)
        .$$(Year.of(1978)).onto(Franchise.Info::setOriginated)
    .onto(Franchise::getInfo)
.get()
```

### fully typed component
Using [org.apache.commons.lang3.Typed](http://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/reflect/Typed.html):
```
Cotterpin.build(Franchise::new)
    .child("Halloween").onto(Franchise::setName)
    .mutate(new TypeLiteral<Franchise.Info>() {})
        .child(Year.of(1978)).onto(Franchise.Info::setOriginated)
    .onto(Franchise::getInfo)
.get()
```

Shorthand version:
```
$(Franchise::new)
    .$$("Halloween").onto(Franchise::setName)
    .__(new TypeLiteral<Franchise.Info>() {})
        .$$(Year.of(1978)).onto(Franchise.Info::setOriginated)
    .onto(Franchise::getInfo)
.get()
```

