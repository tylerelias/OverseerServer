package overseer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;

/*  This class stores all the vital information that the server holds
    and sockets will need to access and modify while the simulation is running.
 */
public class ServerData {
    private Server server;                      // Overseer server data TODO: not needed? remove?
    private String stepNumber;                  // total steps that the simulation will take
    private Integer currentStep;                // the current step in the simulation
    private Integer connectionLimit;            // connection limit set by the Overseer
    private Integer currentConnections;         // current amount of connected sockets
    private final Logger logger = new Logger(); // to log stuff that goes down
    private final ArrayList<ConnectedSockets> connectedSockets; // Puts all the sockets in a nice ArrayList

    ServerData(Server server, String stepNumber, Integer connectionLimit, Integer currentConnections) {
        this.server = server;
        this.stepNumber = stepNumber;
        this.connectionLimit = connectionLimit;
        this.currentConnections = currentConnections;
        this.connectedSockets = new ArrayList<>();
        this.currentStep = 1;
    }

    ServerData() {
        this.server = new Server();
        this.stepNumber = "";
        this.connectionLimit = 0;
        this.currentConnections = 0;
        this.connectedSockets = new ArrayList<>();
        this.currentStep = 1;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(String stepNumber) {
        this.stepNumber = stepNumber;
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

    public int incrementStepOfConnectedSocketByClientId(Integer clientId) {
        for (var val : this.connectedSockets) {
            if(val.getClientId() == clientId)
                return val.incrementCurrentStep();
        }
        throw new NoSuchElementException();
    }

    public int getConnectedSockedStepByClientId(Integer clientId) {
        for (var val : this.connectedSockets) {
            if(val.getClientId() == clientId)
                return val.getCurrentStep();
        }
        throw new NoSuchElementException();
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep++;
    }
}
