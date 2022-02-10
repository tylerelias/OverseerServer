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
    private Integer totalSteps;                  // total steps that the simulation will take
    private AtomicInteger currentStep;                // the current step in the simulation
    private Integer connectionLimit;            // connection limit set by the Overseer
    private Integer currentConnections;         // current amount of connected sockets
    private final Logger logger = new Logger(); // to log stuff that goes down
    private final ArrayList<ConnectedSockets> connectedSockets; // Puts all the sockets in a nice ArrayList

    ServerData(Integer totalSteps, Integer connectionLimit, Integer currentConnections) {
        this.totalSteps = totalSteps;
        this.connectionLimit = connectionLimit;
        this.currentConnections = currentConnections;
        this.connectedSockets = new ArrayList<>();
        this.currentStep = new AtomicInteger(1);
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getConnectionLimit() {
        return connectionLimit;
    }

    public void setConnectionLimit(int connectionLimit) {
        this.connectionLimit = connectionLimit;
    }

    public Integer getCurrentConnections() {
        return currentConnections;
    }

    public void incrementCurrentConnections() {
        this.currentConnections++;
        logger.logCurrentConnections(this.currentConnections);
    }

    public void decrementCurrentConnections() {
        this.currentConnections--;
        logger.logCurrentConnections(this.currentConnections);
    }

    public boolean checkIfAllClientsConnected() {
        return !Objects.equals(getCurrentConnections(), getConnectionLimit());
    }

    public void addSocket(Socket socket, int clientId, int currentStep) {
        this.connectedSockets.add(new ConnectedSockets(socket, clientId, currentStep));
    }

    public ArrayList<ConnectedSockets> getConnectedSockets() {
        return this.connectedSockets;
    }

    public ConnectedSockets getConnectedSocketByClientId(Integer clientId) {
        for (var val : this.connectedSockets) {
            if(val.getClientId() == clientId)
                return val;
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

    public void setCurrentStep(AtomicInteger currentStep) {
        this.currentStep = currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }
}
