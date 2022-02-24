package overseer;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
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
    private final ConcurrentHashMap<UUID, ConnectedSockets> connectedSockets; // Puts all the sockets in a nice ArrayList
    private final ConcurrentLinkedDeque<PersonTransaction> personTransactions;
    private final ConcurrentHashMap<String, PersonInformation> personInformationHashMap;
    private final ConcurrentHashMap<UUID, ArrayList<AccountInformation>> bankInformationHashMap;

    ServerData(Integer totalSteps, Integer connectionLimit, Integer portNumber) {
        this.totalSteps = totalSteps;
        this.connectionLimit = connectionLimit;
        this.portNumber = portNumber;
        this.currentStep = new AtomicInteger(1);
        this.currentConnections = new AtomicInteger(0);
        this.connectedSockets = new ConcurrentHashMap<>();
        this.personTransactions = new ConcurrentLinkedDeque<>();
        this.personInformationHashMap = new ConcurrentHashMap<>();
        this.bankInformationHashMap = new ConcurrentHashMap<UUID, ArrayList<AccountInformation>>();
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

    public void addSocket(Socket socket, UUID clientId) {
        if(this.connectedSockets.containsKey(clientId))
            throw new KeyAlreadyExistsException();
        this.connectedSockets.putIfAbsent(clientId, new ConnectedSockets(socket, clientId, 1));
    }

    public ConcurrentHashMap<UUID, ConnectedSockets> getConnectedSockets() {
        return this.connectedSockets;
    }

    public ConnectedSockets getConnectedSocketByClientId(UUID clientId) {
        for (var socket : this.connectedSockets.values()) {
            if(socket.getClientId() == clientId)
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

    public String convertConnectedClientIdToString() {
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

    public void addBankInformation(UUID clientId, BankInformation bankInformation) {
        this.bankInformationHashMap.putIfAbsent(clientId, bankInformation.getAccountInformation(clientId));
    }

    public boolean haveAllSocketsGottenAClientId() {
        return this.getConnectionLimit() == this.getCurrentConnections().get() &&
                this.getConnectionLimit() == this.getConnectedSockets().size();
    }
}
