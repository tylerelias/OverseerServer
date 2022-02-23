package overseer;

import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectedSockets {
    private final UUID clientId;
    private final Socket socket;
    private final AtomicInteger currentStep;

    ConnectedSockets(Socket socket, UUID clientId, Integer currentStep) {
        this.socket = socket;
        this.currentStep = new AtomicInteger(currentStep);
        this.clientId = clientId;
    }

    public Socket getSocket() {
        return socket;
    }

    public AtomicInteger getCurrentStep() {
        return currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }

    public UUID getClientId() {
        return clientId;
    }
}
