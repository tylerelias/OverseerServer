package overseer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankInformation implements Serializable {
    private final ConcurrentHashMap<String, UUID> informationHashMap = new ConcurrentHashMap<>();

    public void addInformation(String name, UUID clientId) {
        informationHashMap.put(name, clientId);
    }

    public void removeInformation(String name) {
        informationHashMap.remove(name);
    }

    public boolean hasBankName(String name) {
        return informationHashMap.containsKey(name);
    }
}
