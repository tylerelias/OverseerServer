package overseer;

import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Each client that is connected to the simulation is essentially dropped in here as a thread from the Server class.
 * Threadneedle's messages and objects come in here and be processed and worked with and messages are sent through
 * this class to a specific Threadneedle client.
 */
public class ConnectionThread extends Thread {
    private final Socket threadneedleSocket;
    private final Logger logger;
    // There data in serverData is used between Server.java and the threads in ConnectionThread
    private final ServerData serverData;
    private UUID clientId = null;
    private boolean isConnected = true;
    private final AtomicBoolean hasSpawnedThreadneedleThread = new AtomicBoolean(false);
    private boolean isConnectionIdSet;

    public ConnectionThread(Socket threadneedleSocket, ServerData serverData) throws IOException {
        this.threadneedleSocket = threadneedleSocket;
        this.serverData = serverData;
        this.logger = new Logger();
        this.isConnectionIdSet = false;
    }

    /**
     * Socket will keep reading/writing messages as long as the connection to the server is alive
     */
    public void run() {
        // start off by sending the total step to the Threadneedle client
        writeObject(new Messages(Constants.PREFIX_TOTAL_STEPS + this.serverData.getTotalSteps()));

        try {
            while (isConnected && !this.threadneedleSocket.isClosed()) {
                checkMessageQueue();
                if (!hasSpawnedThreadneedleThread.get())
                    spawnThreadneedleReadObject();
                Thread.sleep(1);
            }
            if(this.threadneedleSocket.isConnected()) closeSocket();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if there have been any messages added to the client's message queue
     */
    private void checkMessageQueue() {
        if(isValidClientId()) {
            var client = this.serverData.getConnectedSocketByClientId(this.clientId);
            while (!client.isMessageQueueEmpty()) {
                var object = client.getFromMessageQueue();
                processObject(object);
                client.removeFromMessageQueue(object);
            }
        }
    }

    /**
     * Checks to see what action needs to be taken with the object that is being passed in
     * @param object The object that is being passed in
     */
    private synchronized void processObject(Object object) {
        try {
            if(object.getClass() == Messages.class)
                readMessageObject((Messages) object);

            else if(object.getClass() == PersonTransaction.class)
                processPersonTransaction((PersonTransaction) object);

            else if(object.getClass() == BankInformation.class)
                handleBankInformationObject((BankInformation) object);

        } catch (IOException | InvalidKeyException e) {
            System.out.printf("Overseer::serverConnection() - %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Very trying and fragile function, could need to looking into in the future. But here the BankInformation
     * object gets updated in the ServerData class, and it gets sent to the connected client
     * @param bankInformation The incoming object with the BankIformation data
     */
    private void handleBankInformationObject(BankInformation bankInformation) {
        if(this.serverData.getReadyClients() == this.serverData.getConnectionLimit()) {
            this.serverData.getBankInformationHashMap().addBankInformation(bankInformation);
            writeObject(bankInformation);
        }
        else
          this.serverData.getBankInformationHashMap().addBankInformation(bankInformation);
    }

    /**
     * Because the readObject() is a blocking call, it is spawned with a new Thread.
     * This is quite expensive and resource intensive, but the only thing I was
     * able to come up with at the time
     */
    private void spawnThreadneedleReadObject() {
        this.hasSpawnedThreadneedleThread.set(true);
        new Thread(() -> {
            try {
                readObject(this.threadneedleSocket);
                this.hasSpawnedThreadneedleThread.set(false);
            } catch (InvalidObjectException e) {
                System.err.println("Thread spawn failure, likely because of a disconnected socket");
            }
        }).start();
    }

    /**
     * A blocking call that reads the object input stream and passes it in to processObject
     * @param socket the socket being read from
     */
    private void readObject(Socket socket) throws InvalidObjectException {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            Object object = objectInputStream.readObject();

            if(!isValidClientId())
                processObject(object);

            else {
                var client = this.serverData.getConnectedSocketByClientId(this.clientId);
                client.addToMessageQueue(object);
            }
        } catch (Exception e) {
            throw new InvalidObjectException("Object could not be read");
        }
    }

    /**
     * Incoming PersonTransaction object to the client means that the Threadneedle instance is sending
     * a pending deposit request, so the transaction is stored to the server and then the object is sent
     * to the ClientTo
     * @param personTransaction transaction details
     */
    private void processPersonTransaction(PersonTransaction personTransaction) {
        this.serverData.addPersonTransaction((personTransaction));

        if(this.clientId.equals(personTransaction.getClientIdTo())) {
            this.serverData.addPersonTransaction(personTransaction);
            writeObject(personTransaction);
        }

        if(this.clientId.equals(personTransaction.getClientIdFrom()))
            addToClientMessageQueue(personTransaction.getClientIdTo(), personTransaction);
    }

    /**
     * Close the socket and remove it from the serverData's currently connected sockets
     */
    private void closeSocket() throws IOException {
        this.threadneedleSocket.close();
        this.serverData.decrementCurrentConnections();

        if(this.serverData.removeSocketByClientId(this.clientId) == null)
            logger.logErrorSocketNotInSocketList(this.clientId.toString());
        else
            logger.logSocketClosed(this.clientId.toString());
    }

    private boolean checkIfConnectionTerminated(String message) {
        return !(message.equals(Constants.TERMINATE_CONNECTION) ||
                message.equals(Constants.ABORT_CONNECTION) ||
                message.equals(Constants.DISCONNECTED));
    }

    /**
     * Takes in a string message and checks what type of command it is and calls the correct function
     * according to the command type
     * @param messages the incoming message that is being parsed
     */
    private void readMessageObject(Messages messages) throws IOException, InvalidKeyException {
        var splitMessage = messages.getMessage().split(Constants.COMMAND_SPLITTER);

        for (var word : splitMessage) {
            if (!isValidClientId()) {
                this.isConnectionIdSet = checkForConnectionId(word);
                if (this.isConnectionIdSet) break;
            }
            else {
                // PREFIX_NEXT_STEP comes from the server about what the next step N will be
                // this message is passed on to the Threadneedle client
                if (word.contains(Constants.PREFIX_NEXT_STEP))
                    writeObject(messages);
                else if(word.contains(Constants.PREFIX_CURRENT_STEP))
                    setSteps(splitMessage);
                else if(word.contains(Constants.PREFIX_TRANSACTION_DONE))
                    this.serverData.removePersonTransaction(UUID.fromString(word.split(Constants.COLON)[1]));
                else if (word.contains(Constants.PREFIX_TRANSACTION_FAILED))
                    revertIncompleteTransfer(splitMessage);
                else if(word.contains(Constants.COMMAND_ALL_CLIENTS_CONNECTED))
                    writeObject(messages);
                else if(word.contains(Constants.COMMAND_SIMULATION_COMPLETED))
                    writeObject(messages);
                //todo: very prone if same client sends 2x, fix
                else if(word.contains(Constants.PREFIX_CLIENT_READY))
                    this.serverData.incrementReadyClients();
                else
                    this.isConnected = checkIfConnectionTerminated(word);
            }
        }
//        if(this.clientId != null)
//            logger.logSocketMessage(messages.getMessage(), clientId.toString());
    }

    /**
     * If a transfer does not go through to its recipient, the sender will be notified to be able to cancel the withdrawal
     * @param splitMessage
     * @throws InvalidKeyException if no transaction with the given ID is found, the exception is thrown
     */
    private void revertIncompleteTransfer(String[] splitMessage) throws InvalidKeyException {
        UUID transactionId = null;
        for(var word : splitMessage) {
            if(word.contains(Constants.PREFIX_TRANSACTION_ID))
                transactionId = UUID.fromString(word.split(Constants.COLON)[1]);
        }
        if(transactionId == null) throw new InvalidKeyException("Transaction ID not found");

        PersonTransaction personTransaction = this.serverData.getPersonTransactionById(transactionId);
        addToClientMessageQueue(personTransaction.getClientIdFrom(), new Messages(
                Constants.PREFIX_REVERT_TRANSACTION + transactionId
        ));
    }

    private void removeCompletedTransaction(String[] splitMessage) {
        for(var word : splitMessage) {
            if(word.contains(Constants.PREFIX_TRANSACTION_ID)) {
                UUID transactionId = UUID.fromString(word.split(Constants.COLON)[1]);
                this.serverData.removePersonTransaction(transactionId);
            }
        }
    }

    /**
     * This functions sends objects to another client based on the ID that is passed in
     * @param clientId the client queue that it will be added to
     * @param object the object being added
     */
    private synchronized void addToClientMessageQueue(UUID clientId, Object object) {
        var clientSocket = this.serverData.getConnectedSocketByClientId(clientId);
        clientSocket.addToMessageQueue(object);
    }

    /**
     * Sends the object to the ConnectionThread's socket
     * @param object
     */
    public synchronized void writeObject(Object object) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(threadneedleSocket.getOutputStream());
            outputStream.writeObject(object);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("WriteObject error, likely because of a disconnected socket");
        }
    }

    private boolean isValidClientId() {
        return this.isConnectionIdSet && this.clientId != null;
    }

    /**
     * Validate that the server's current step matches the client's current step
     * @param completedStep
     * @return true of step is valid, false if not
     */
    private boolean validateSteps(Integer completedStep) {
        return  Objects.equals(this.serverData.getConnectedSockedStepByClientId(this.clientId),
                this.serverData.getCurrentStep().get()) ||
                completedStep == this.serverData.getCurrentStep().get();
    }

    /**
     * Parse and validate the step being passed in
     * @param splitMessage
     */
    private void setSteps(String[] splitMessage)  {
        for(var word : splitMessage) {
            if (word.contains(Constants.PREFIX_CURRENT_STEP)) {
                var completedStep = Integer.valueOf(word.split(Constants.COLON)[1]);

                if (validateSteps(completedStep))
                    this.serverData.incrementStepOfConnectedSocketByClientId(this.clientId);
                else
                    logger.logStepMismatchError(completedStep, this.serverData.getCurrentStep().get(), this.clientId.toString());

            }
        }
    }

    /**
     * This sets the ClientID of the socket that the Threadneedle program is connected to
     * @param word
     * @return
     */
    private boolean checkForConnectionId(String word) {
        if (word.contains(Constants.PREFIX_SET_CLIENT_ID)) {
            UUID clientId = UUID.fromString(word.split(Constants.COLON)[1]);
            setClientId(clientId);
            writeObject(new Messages(Constants.PREFIX_RECEIVED_CLIENT_ID + this.clientId));
            this.serverData.addConnectedSocket(this.threadneedleSocket, this.clientId);
            return true;
        }
        return false;
    }

    private void setClientId(UUID clientId) {
        this.clientId = clientId;
        logger.logClientIdSet(this.clientId.toString());
    }
}
