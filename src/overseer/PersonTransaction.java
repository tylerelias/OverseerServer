package overseer;

import java.util.UUID;

public class PersonTransaction {
    UUID transactionId;
    UUID clientIdTo;      // The ID of the Threadneedle client connected to Overseer
    UUID clientIdFrom;
    String bankName;
    String personId;        // ID of the person that is being transferred to
    long amount;
    Integer step;           // at what step does the transaction take place

    public PersonTransaction(UUID clientIdTo, UUID clientIdFrom, String personId, long amount, Integer step, String bankName) {
        this.clientIdTo = clientIdTo;
        this.clientIdFrom = clientIdFrom;
        this.personId = personId;
        this.amount = amount;
        transactionId = UUID.randomUUID();
        this.step = step;
        this.bankName = bankName;
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

    public String getBankName() { return bankName; }

    @Override
    public String toString() {
        return Constants.PREFIX_DEPOSIT_TO + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_TRANSACTION_ID + transactionId + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_CLIENT_TO + clientIdTo + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_CLIENT_FROM + clientIdFrom + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_PERSON_NAME + personId + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_BANK_NAME + bankName + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_AMOUNT + amount + Constants.COMMAND_SPLITTER +
                Constants.PREFIX_TRANSFER_AT_STEP + step;
    }
}
