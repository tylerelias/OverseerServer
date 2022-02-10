package overseer;

import java.net.Socket;

public class ConnectedSockets {
    private int clientId;
    private Socket socket;
    private int currentStep;

    ConnectedSockets(Socket socket, int clientId, int currentStep) {
        this.socket = socket;
        this.currentStep = currentStep;
        this.clientId = clientId;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
