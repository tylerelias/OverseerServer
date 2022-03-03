package overseer;

import java.io.Serializable;
import java.util.UUID;

public class PersonTransaction implements Serializable {
    UUID transactionId;
    UUID clientIdTo;      // The ID of the Threadneedle client connected to Overseer
    UUID clientIdFrom;
    Integer bankId;
    String personId;        // ID of the person that is being transferred to
    long amount;
    Integer step;           // at what step does the transaction take place

    public PersonTransaction(UUID clientIdTo, UUID clientIdFrom, String personId, long amount, Integer step, Integer bankId) {
        this.clientIdTo = clientIdTo;
        this.clientIdFrom = clientIdFrom;
        this.personId = personId;
        this.amount = amount;
        transactionId = UUID.randomUUID();
        this.step = step;
        this.bankId = bankId;
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

    public String getPersonId() {
        return personId;
    }

    public long getAmount() {
        return amount;
    }

    public Integer getStep() {
        return step;
    }

    public Integer getBankId() { return bankId; }

    @Override
    public String toString() {
        return Constants.PREFIX_DEPOSIT_TO + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_TRANSACTION_ID + transactionId + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_CLIENT_TO + clientIdTo + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_CLIENT_FROM + clientIdFrom + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_PERSON_NAME + personId + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_BANK_NAME + bankId + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_AMOUNT + amount + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_TRANSFER_AT_STEP + step;
    }
}
