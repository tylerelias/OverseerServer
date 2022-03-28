package overseer;

import java.io.Serializable;
import java.util.UUID;

public class TouristTransaction implements Serializable {
    UUID transactionId;
    UUID clientId; // ID of the Threadneedle client that the Tourist is from
    Integer bankId;
    String personId;
    long amount;
    Integer step;

    public TouristTransaction(UUID clientId, Integer bankId, String personId, long amount, Integer step) {
        this.transactionId = UUID.randomUUID();
        this.clientId = clientId;
        this.bankId = bankId;
        this.personId = personId;
        this.amount = amount;
        this.step = step;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Integer getBankId() {
        return bankId;
    }

    public String getPersonId() {
        return personId;
    }

    public long getAmount() {
        return amount;
    }
}
