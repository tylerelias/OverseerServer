package overseer;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class PersonInformation implements Serializable {
    private final ConcurrentHashMap<String, String> informationHashMap = new ConcurrentHashMap<>();

    public void addInformation(String name, String clientId) {
        informationHashMap.put(name, clientId);
    }

    public void removeInformation(String name) {
        informationHashMap.remove(name);
    }

    public boolean hasPersonName(String name) {
        return informationHashMap.containsKey(name);
    }
}
