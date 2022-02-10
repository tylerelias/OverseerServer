package overseer;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectedSockets {
    private int clientId;
    private Socket socket;
    private AtomicInteger currentStep;

    ConnectedSockets(Socket socket, int clientId, Integer currentStep) {
        this.socket = socket;
        this.currentStep = new AtomicInteger(currentStep);
        this.clientId = clientId;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public AtomicInteger getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = new AtomicInteger(currentStep);
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
