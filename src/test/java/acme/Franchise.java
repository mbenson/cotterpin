package acme;

import java.time.Year;
import java.util.Map;

public class Franchise {
    public static class Info {
        private Year originated;

        public Year getOriginated() {
            return originated;
        }

        public void setOriginated(Year originated) {
            this.originated = originated;
        }
    }

    private String name;
    private Map<String, Character> characters;
    private Info info = new Info();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Character> getCharacters() {
        return characters;
    }

    public void setCharacters(Map<String, Character> characters) {
        this.characters = characters;
    }

    public Info getInfo() {
        return info;
    }
}
