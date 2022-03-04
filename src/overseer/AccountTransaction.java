package overseer;

import java.io.Serializable;
import java.util.UUID;

public class AccountTransaction implements Serializable {
    UUID transactionId;

    UUID clientIdTo;            // The ID of the Threadneedle client connected to Overseer
    Integer bankIdTo;
    String peronIdTo;        // ID of the person that is being transferred to

    long amountTo;

    UUID clientIdFrom;
    Integer bankIdFrom;
    String personIdFrom;

    Integer step;           // at what step does the transaction take place

    public AccountTransaction(
            UUID clientIdTo,
            String peronIdTo,
            Integer bankIdTo,
            long amountTo,
            UUID clientIdFrom,
            Integer bankIdFrom,
            String personIdFrom,
            Integer step
    ) {
        transactionId = UUID.randomUUID();

        this.clientIdTo = clientIdTo;
        this.peronIdTo = peronIdTo;
        this.bankIdTo = bankIdTo;
        this.amountTo = amountTo;

        this.clientIdFrom = clientIdFrom;
        this.bankIdFrom = bankIdFrom;
        this.personIdFrom = personIdFrom;

        this.step = step;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getClientIdTo() {
        return clientIdTo;
    }

    public UUID getClientIdFrom() {
        return clientIdFrom;
    }

    public String getPeronIdTo() {
        return peronIdTo;
    }

    public long getAmountTo() {
        return amountTo;
    }

    public Integer getStep() {
        return step;
    }

    public Integer getBankIdTo() { return bankIdTo; }

    public void setClientIdFrom(UUID clientIdFrom) {
        this.clientIdFrom = clientIdFrom;
    }

    public Integer getBankIdFrom() {
        return bankIdFrom;
    }

    public void setBankIdFrom(Integer bankIdFrom) {
        this.bankIdFrom = bankIdFrom;
    }

    public String getPersonIdFrom() {
        return personIdFrom;
    }

    public void setAmountTo(long amountTo) {
        this.amountTo = amountTo;
    }

    public void setPersonIdFrom(String personIdFrom) {
        this.personIdFrom = personIdFrom;
    }
}
