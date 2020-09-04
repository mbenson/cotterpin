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
package acme;

import java.time.Year;
import java.util.Map;
import java.util.Optional;

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
    private String studio;

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

    public void setInfo(Info info) {
        this.info = info;
    }

    public String getStudio() {
        return studio;
    }

    public void maybeSetStudio(Optional<String> studio) {
        this.studio = studio.orElse(null);
    }
}
