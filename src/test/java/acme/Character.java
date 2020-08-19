package acme;

import java.util.Set;

public class Character {
    private CharacterType type;
    private Set<String> weaknesses;

    public CharacterType getType() {
        return type;
    }

    public void setType(CharacterType type) {
        this.type = type;
    }

    public Set<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(Set<String> weaknesses) {
        this.weaknesses = weaknesses;
    }
}
