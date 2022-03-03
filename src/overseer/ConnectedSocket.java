package overseer;

import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores all the essential information for the connected client.
 * The usage of the class is in ServerData, where it is stored in a HashMap<ClientID, ConnectionThread>
 */
public class ConnectedSocket {
    private final UUID clientId;

    private final Socket threadneedleSocket;
    private final AtomicInteger currentStep;
    private final ConcurrentLinkedQueue<Object> messageQueue = new ConcurrentLinkedQueue<>();

    ConnectedSocket(Socket threadneedleSocket, UUID clientId, Integer currentStep) {
        this.threadneedleSocket = threadneedleSocket;
        this.currentStep = new AtomicInteger(currentStep);
        this.clientId = clientId;
    }

    public Socket getThreadneedleSocket() {
        return threadneedleSocket;
    }

    public int getCurrentStep() {
        return currentStep.get();
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }

    public UUID getClientId() {
        return clientId;
    }

    public void addToMessageQueue(Object object) {
        messageQueue.add(object);
    }

    public Object getFromMessageQueue() {
        return messageQueue.peek();
    }

    public void removeFromMessageQueue(Object object) {
        messageQueue.remove(object);
    }

    public ConcurrentLinkedQueue<Object> getMessageQueue() {
        return messageQueue;
    }

    public boolean isMessageQueueEmpty() {
        return messageQueue.isEmpty();
    }
}
