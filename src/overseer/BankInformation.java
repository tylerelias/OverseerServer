package overseer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankInformation implements Serializable {

    private final ConcurrentHashMap<UUID, ArrayList<AccountInformation>> informationHashMap = new ConcurrentHashMap<>();

    //todo: refactor
    public void addToBankInformationItem(UUID clientId, AccountInformation accountInformation) {
        ArrayList<AccountInformation> copy = this.informationHashMap.get(clientId);
        if(copy != null) {
            copy.add(accountInformation);
            informationHashMap.put(clientId, copy);
        }
        else {
            ArrayList<AccountInformation> a = new ArrayList<>();
            a.add(accountInformation);
            informationHashMap.put(clientId, a);
        }
    }

    public void addToBankInformationList(UUID clientId, ArrayList<AccountInformation> accountInformation) {
        ArrayList<AccountInformation> copy = this.informationHashMap.get(clientId);
        if(copy != null) {
            copy.addAll(accountInformation);
            informationHashMap.put(clientId, copy);
        }
        else
            informationHashMap.put(clientId, accountInformation);
    }

    public ConcurrentHashMap<UUID, ArrayList<AccountInformation>> getInformationHashMap() {
        return informationHashMap;
    }

    public ArrayList<AccountInformation> getAccountInformation(UUID clientId) {
        return informationHashMap.get(clientId);
    }
}
