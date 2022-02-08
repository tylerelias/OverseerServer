package overseer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class ServerData {
    private Server server;
    private String stepNumber;
    private Integer connectionLimit;
    private Integer currentConnections;
    private final Logger logger = new Logger();
    private final ArrayList<Socket> connectedSockets;

    ServerData(Server server, String stepNumber, Integer connectionLimit, Integer currentConnections) {
        this.server = server;
        this.stepNumber = stepNumber;
        this.connectionLimit = connectionLimit;
        this.currentConnections = currentConnections;
        this.connectedSockets = new ArrayList<>();
    }

    ServerData() {
        this.server = new Server();
        this.stepNumber = "";
        this.connectionLimit = 0;
        this.currentConnections = 0;
        this.connectedSockets = new ArrayList<>();
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

    public void setCurrentConnections(Integer currentConnections) {
        this.currentConnections = currentConnections;
        logger.logCurrentConnections(currentConnections);
    }

    public boolean checkIfAllClientsConnected() {
        return !Objects.equals(getCurrentConnections(), getConnectionLimit());
    }

    public void addSocket(Socket socket) {
        this.connectedSockets.add(socket);
    }

    public boolean removeSocket(Socket socket) {
        return this.connectedSockets.remove(socket);
    }

    public ArrayList<Socket> getConnectedSockets() {
        return this.connectedSockets;
    }
}
