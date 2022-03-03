package overseer;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*  This class stores all the vital information that the server holds
    and sockets will need to access and modify while the simulation is running.
 */
public class ServerData {
    private final Integer totalSteps;                  // total steps that the simulation will take
    private final AtomicInteger currentStep;                // the current step in the simulation
    private final Integer connectionLimit;            // connection limit set by the Overseer
    private final AtomicInteger currentConnections;         // current amount of connected sockets
    // when a client has loaded its configurations and env. it will send a "ClientReady:ClientID" to the server
    private final AtomicInteger readyClients;
    private final Integer portNumber;                   // the port number of the server itself
    private final Logger logger = new Logger(); // to log stuff that goes down
    // the sockets (threadneedle programs) that are connected, along with important information about them
    private final ConcurrentHashMap<UUID, ConnectedSockets> connectedSockets;
    // keeps track of the pending transactions that are taking place between clients
    private final ConcurrentHashMap<UUID, PersonTransaction> pendingTransactions;
    // the finished transactions of the simulation
    private final ConcurrentHashMap<UUID, PersonTransaction> completedTransactions;
    // Stores information about all banks that the clients have in their simulation
    private final BankInformation bankInformationHashMap;
    private final AtomicBoolean hasSimulationStarted = new AtomicBoolean(false);

    ServerData(Integer totalSteps, Integer connectionLimit, Integer portNumber) {
        this.totalSteps = totalSteps;
        this.connectionLimit = connectionLimit;
        this.portNumber = portNumber;
        this.currentStep = new AtomicInteger(1);
        this.currentConnections = new AtomicInteger(0);
        this.connectedSockets = new ConcurrentHashMap<>();
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.bankInformationHashMap = new BankInformation();
        completedTransactions = new ConcurrentHashMap<>();
        readyClients = new AtomicInteger(0);
    }

    public void incrementReadyClients() {
        this.readyClients.incrementAndGet();
    }

    public int getReadyClients() {
        return this.readyClients.get();
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

    public void addConnectedSocket(Socket threadneedleSocket, UUID clientId) {
        if(this.connectedSockets.containsKey(clientId))
            throw new KeyAlreadyExistsException();
        this.connectedSockets.putIfAbsent(clientId, new ConnectedSockets(threadneedleSocket, clientId, 1));
    }

    public ConcurrentHashMap<UUID, ConnectedSockets> getConnectedSockets() {
        return this.connectedSockets;
    }

    public ConnectedSockets getConnectedSocketByClientId(UUID clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId().equals(clientId))
                return socket;
        }
        throw new NoSuchElementException();
    }

    public ConnectedSockets removeSocketByClientId(UUID clientId) {
        return this.connectedSockets.remove(clientId);
    }

    public void incrementStepOfConnectedSocketByClientId(UUID clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId) {
                socket.incrementCurrentStep();
                return;
            }
        }
        throw new NoSuchElementException(String.format("Socket with Client ID %s not found%n", clientId));
    }

    public int getConnectedSockedStepByClientId(UUID clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId)
                return socket.getCurrentStep();
        }
        throw new NoSuchElementException(String.format("Socket with Client ID %s not found%n", clientId));
    }

    public void closeAllSockets() {
        this.getConnectedSockets().values().forEach(s -> {
            try {
                if(s.getThreadneedleSocket().isConnected())
                    s.getThreadneedleSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public String convertConnectedClientIdToUUID() {
        StringBuilder convertedClientIds = new StringBuilder();
        ArrayList<UUID> clientIds = new ArrayList<>(connectedSockets.keySet());

        clientIds.forEach(key -> convertedClientIds.append(key).append(","));

        return convertedClientIds.toString();
    }

    public AtomicInteger getCurrentStep() {
        return this.currentStep;
    }

    public void incrementCurrentStep() {
        this.currentStep.incrementAndGet();
    }

    public void addPersonTransaction(PersonTransaction personTransaction) {
        this.pendingTransactions.put(personTransaction.transactionId, personTransaction);
    }

    public void removePersonTransaction(UUID transactionId) {
        this.pendingTransactions.remove(transactionId);
    }

    public boolean isPersonTransactionEmpty() {
        return this.pendingTransactions.isEmpty();
    }

    public ConcurrentHashMap<UUID, PersonTransaction> getPendingTransactions() {
        return pendingTransactions;
    }

    public PersonTransaction getPersonTransactionById(UUID transactionId) {
        return this.pendingTransactions.get(transactionId);
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

    public boolean haveAllClientsBeenInitialized() {
        return this.getConnectionLimit() == this.getCurrentConnections().get() &&
                this.getConnectionLimit() == this.getConnectedSockets().size() &&
                this.getConnectionLimit() == this.getReadyClients();
    }

    public BankInformation getBankInformationHashMap() {
        return bankInformationHashMap;
    }

    public void setHasSimulationStarted(boolean hasSimulationStarted) {
        this.hasSimulationStarted.set(hasSimulationStarted);
    }

    public boolean getHasSimulationStarted() {
        return this.hasSimulationStarted.get();
    }
}
