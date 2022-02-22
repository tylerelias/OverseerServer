package overseer;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/*  This class stores all the vital information that the server holds
    and sockets will need to access and modify while the simulation is running.
 */
public class ServerData {
    private final Integer totalSteps;                  // total steps that the simulation will take
    private final AtomicInteger currentStep;                // the current step in the simulation
    private final Integer connectionLimit;            // connection limit set by the Overseer
    private final AtomicInteger currentConnections;         // current amount of connected sockets
    private final Integer portNumber;
    private final Logger logger = new Logger(); // to log stuff that goes down
    private final ConcurrentHashMap<Integer, ConnectedSockets> connectedSockets; // Puts all the sockets in a nice ArrayList
    private final ConcurrentLinkedDeque<PersonTransaction> personTransactions;
    private final ConcurrentHashMap<String, PersonInformation> personInformationHashMap;

    ServerData(Integer totalSteps, Integer connectionLimit, Integer portNumber) {
        this.totalSteps = totalSteps;
        this.connectionLimit = connectionLimit;
        this.portNumber = portNumber;
        this.currentStep = new AtomicInteger(1);
        this.currentConnections = new AtomicInteger(0);
        this.connectedSockets = new ConcurrentHashMap<>();
        this.personTransactions = new ConcurrentLinkedDeque<>();
        this.personInformationHashMap = new ConcurrentHashMap<>();
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

    public void addSocket(Socket socket, int clientId) {
        if(this.connectedSockets.containsKey(clientId))
            throw new KeyAlreadyExistsException();
        this.connectedSockets.putIfAbsent(clientId, new ConnectedSockets(socket, clientId, 1));
    }

    public ConcurrentHashMap<Integer, ConnectedSockets> getConnectedSockets() {
        return this.connectedSockets;
    }

    public ConnectedSockets getConnectedSocketByClientId(Integer clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId)
                return socket;
        }
        throw new NoSuchElementException();
    }

    public ConnectedSockets removeSocketByClientId(Integer clientId) {
        return this.connectedSockets.remove(clientId);
    }

    public void incrementStepOfConnectedSocketByClientId(Integer clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId) {
                socket.incrementCurrentStep();
                return;
            }
        }
        throw new NoSuchElementException(String.format("Socket with Client ID %s not found%n", clientId));
    }

    public int getConnectedSockedStepByClientId(Integer clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId)
                return socket.getCurrentStep().get();
        }
        throw new NoSuchElementException(String.format("Socket with Client ID %s not found%n", clientId));
    }

    public void closeAllSockets() {
        this.getConnectedSockets().values().forEach(s -> {
            try {
                if(s.getSocket().isConnected())
                    s.getSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public AtomicInteger getCurrentStep() {
        return this.currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }

    public void addPersonTransaction(PersonTransaction personTransaction) {
        this.personTransactions.add(personTransaction);
    }

    public ConcurrentLinkedDeque<PersonTransaction> getPersonTransactions() {
        return personTransactions;
    }

    public Integer getPortNumber() {
        return portNumber;
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

    public void addClientInformation(String clientId, PersonInformation personInformation) {
        this.personInformationHashMap.putIfAbsent(clientId, personInformation);
    }
}
