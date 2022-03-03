package overseer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankInformation implements Serializable {

    private final ConcurrentHashMap<UUID, HashMap<Integer, AccountInformation>> bankInformationHashMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<UUID, HashMap<Integer, AccountInformation>> getBankInformationHashMap() {
        return bankInformationHashMap;
    }

    public void addAccountInformationByClientId(UUID clientId, HashMap<Integer, AccountInformation> accountInformation) {
        if(bankInformationHashMap.get(clientId) == null)
            bankInformationHashMap.put(clientId, accountInformation);
        else
            for(AccountInformation item : accountInformation.values())
                bankInformationHashMap.get(clientId).putIfAbsent(item.getAccountId(), item);
    }

    public HashMap<String, AccountInformation> getAccountInformationByClientId(UUID clientId) {
        Collection<AccountInformation> accountInformation = bankInformationHashMap.get(clientId).values();
        HashMap<String, AccountInformation> informationHashMap = new HashMap<>();
        accountInformation.forEach((account -> informationHashMap.put(account.getOwnerId(), account)));
        return informationHashMap;
    }

    // Todo: Explain
    public void addBankInformation(BankInformation bankInformation) {
        this.bankInformationHashMap.putAll(bankInformation.getBankInformationHashMap());
    }
}
