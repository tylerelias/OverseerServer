package overseer;

import java.util.UUID;

public class Debug {

    Debug() {};

    // Server.java

    public void serverSendAllClientsObject(Object object) {
        System.out.println("Sending all clients an objects of the type: " + object.getClass());
    }

    public void serverAcceptConnections(int currentConnections, int connectionLimit) {
        System.out.println("Server is now accepting connections "
                + currentConnections + " / "
                + connectionLimit);
    }

    public void serverTellAllClientsSimulationIsCompleted() {
        System.out.println("Letting all clients know that simulation is completed");
    }

    // ConnectionThread.java
    public void connectionThreadReadMessageObject(UUID clientId, Messages messages) {
        System.out.println(clientId + ": got the message: " + messages.getMessage());
    }

    public void connectionThreadProcessObject(UUID clientId, Object object) {
        System.out.println(object.getClass() + " is being processed by Client: " + clientId);
    }

    public void connectionThreadCheckMessageQueue(UUID clientId, Object object) {
        System.out.println("Processing object from message queue for client: " + clientId +
                ", object type: " + object.getClass());
    }

    public void connectionThreadRun(UUID clientId, int totalSteps) {
        System.out.println(clientId +
                ", sending client that the Total steps are: " +
                totalSteps);
    }

    public void connectionThreadProcessAccountTransaction(UUID clientId, AccountTransaction accountTransaction) {
        System.out.println("========================");
        System.out.println(clientId + ", got an Account transaction object ");
        System.out.println("Transaction ID: " + accountTransaction.getTransactionId());
        System.out.println("From Client: " + accountTransaction.getClientIdFrom());
        System.out.println("From Person: " + accountTransaction.getPersonIdFrom());
        System.out.println("From Bank: " + accountTransaction.getBankIdFrom());
        System.out.println("To Client: " + accountTransaction.getClientIdTo());
        System.out.println("To Person: " + accountTransaction.getPeronIdTo());
        System.out.println("To Bank: " + accountTransaction.getBankIdTo());
        System.out.println("To Amount: " + accountTransaction.getAmountTo());
        System.out.println("========================");
    }
}
