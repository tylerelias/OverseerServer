package overseer;

import java.util.UUID;

public class PersonTransaction {
    UUID transactionId;
    String clientIdTo;      // The ID of the Threadneedle client connected to Overseer
    String clientIdFrom;
    String personId;        // ID of the person that is being transferred to
    Integer amount;
    Integer step;           // at what step does the transaction take place

    public PersonTransaction(String clientIdTo, String clientIdFrom, String personId, Integer amount, Integer step) {
        this.clientIdTo = clientIdTo;
        this.clientIdFrom = clientIdFrom;
        this.personId = personId;
        this.amount = amount;
        transactionId = UUID.randomUUID();
        this.step = step;
    }
}
