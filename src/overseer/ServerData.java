package overseer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/*  This class stores all the vital information that the server holds
    and sockets will need to access and modify while the simulation is running.
 */
public class ServerData {
    private final Integer totalSteps;                  // total steps that the simulation will take
    private final AtomicInteger currentStep;                // the current step in the simulation
    private final Integer connectionLimit;            // connection limit set by the Overseer
    private final AtomicInteger currentConnections;         // current amount of connected sockets
    private final Logger logger = new Logger(); // to log stuff that goes down
    private final ArrayList<ConnectedSockets> connectedSockets; // Puts all the sockets in a nice ArrayList

    ServerData(Integer totalSteps, Integer connectionLimit, Integer currentConnections) {
        this.totalSteps = totalSteps;
        this.connectionLimit = connectionLimit;
        this.currentConnections = new AtomicInteger(currentConnections);
        this.connectedSockets = new ArrayList<>();
        this.currentStep = new AtomicInteger(1);
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public Integer getConnectionLimit() {
        return connectionLimit;
    }

    public AtomicInteger getCurrentConnections() {
        return currentConnections;
    }

    public void incrementCurrentConnections() {
        this.currentConnections.incrementAndGet();
        logger.logCurrentConnections(this.currentConnections.get());
    }

    public void decrementCurrentConnections() {
        this.currentConnections.decrementAndGet();
        logger.logCurrentConnections(this.currentConnections.get());
    }

    public boolean checkIfAllClientsConnected() {
        return !Objects.equals(getCurrentConnections().get(), getConnectionLimit());
    }

    public void addSocket(Socket socket, int clientId, int currentStep) {
        this.connectedSockets.add(new ConnectedSockets(socket, clientId, currentStep));
    }

    public ArrayList<ConnectedSockets> getConnectedSockets() {
        return this.connectedSockets;
    }

    public ConnectedSockets getConnectedSocketByClientId(Integer clientId) {
        for (var socket : this.connectedSockets) {
            if(socket.getClientId() == clientId)
                return socket;
        }
        throw new NoSuchElementException();
    }

    public boolean removeSocketByClientId(Integer clientId) {
        return this.connectedSockets.removeIf(val -> val.getClientId() == clientId);
    }

    public void incrementStepOfConnectedSocketByClientId(Integer clientId) {
        for (var socket : this.connectedSockets) {
            if(socket.getClientId() == clientId) {
                socket.incrementCurrentStep();
                return;
            }
        }
        throw new NoSuchElementException();
    }

    public int getConnectedSockedStepByClientId(Integer clientId) {
        for (var socket : this.connectedSockets) {
            if(socket.getClientId() == clientId)
                return socket.getCurrentStep().get();
        }
        throw new NoSuchElementException();
    }

    public AtomicInteger getCurrentStep() {
        return this.currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }
}
