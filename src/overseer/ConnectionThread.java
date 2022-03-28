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
    private final Debug debug = new Debug();

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

                if(this.serverData.isDebugEnabled())
                    debug.connectionThreadCheckMessageQueue(this.clientId, object);
            }
        }
    }

    /**
     * Checks to see what action needs to be taken with the object that is being passed in
     * @param object The object that is being passed in
     */
    private synchronized void processObject(Object object) {
        if(this.serverData.isDebugEnabled())
            debug.connectionThreadProcessObject(this.clientId, object);
        try {
            if(object.getClass() == Messages.class)
                readMessageObject((Messages) object);

            else if(object.getClass() == AccountTransaction.class)
                processAccountTransaction((AccountTransaction) object);

            else if(object.getClass() == BankInformation.class)
                handleBankInformationObject((BankInformation) object);

            else if(object.getClass() == TouristTransaction.class)
                processTouristTransaction((TouristTransaction) object);

        } catch (IOException | InvalidKeyException e) {
            System.err.printf("Overseer::serverConnection() - %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private void processTouristTransaction(TouristTransaction touristTransaction) {
        if(touristTransaction.getClientId().equals(this.clientId)) {
            this.serverData.addPendingTouristTransaction(touristTransaction);
            writeObject(touristTransaction);
        }
        else this.serverData
                    .getConnectedSocketByClientId(touristTransaction.getClientId())
                    .addToMessageQueue(touristTransaction);
    }

    /**
     * Very trying and fragile function, could need to looking into in the future. But here the BankInformation
     * object gets updated in the ServerData class, and it gets sent to the connected client
     * @param bankInformation The incoming object with the BankInformation data
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
                e.printStackTrace();
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
     * @param accountTransaction transaction details
     */
    private void processAccountTransaction(AccountTransaction accountTransaction) {
        if(this.serverData.isDebugEnabled())
            debug.connectionThreadProcessAccountTransaction(this.clientId, accountTransaction);

        this.serverData.addToPendingTransactions((accountTransaction));

        if(this.clientId.equals(accountTransaction.getClientIdTo())) {
            this.serverData.addToPendingTransactions(accountTransaction);
            writeObject(accountTransaction);
        }

        if(this.clientId.equals(accountTransaction.getClientIdFrom()))
            addToClientMessageQueue(accountTransaction.getClientIdTo(), accountTransaction);
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
        return !(message.equals(Constant.TERMINATE_CONNECTION));
    }

    /**
     * Takes in a string message and checks what type of command it is and calls the correct function
     * according to the command type
     * @param messages the incoming message that is being parsed
     */
    private void readMessageObject(Messages messages) throws IOException, InvalidKeyException {
        if (this.serverData.isDebugEnabled())
            debug.connectionThreadReadMessageObject(this.clientId, messages);

        var splitMessage = messages.getMessage().split(Constant.COMMAND_SPLITTER);

        for (var word : splitMessage) {
            if (!isValidClientId()) {
                this.isConnectionIdSet = checkForConnectionId(word);
                if (this.isConnectionIdSet) break;
            }
            else {
                if (word.contains(Constant.PREFIX_NEXT_STEP)) {
                    writeObject(messages);
                    break;
                }
                if (word.contains(Constant.PREFIX_TAKE_STEP)) {
                    writeObject(messages);
                    break;
                }
                else if(word.contains(Constant.PREFIX_CURRENT_STEP)) {
                    setSteps(word);
                    break;
                }
                else if(word.contains(Constant.PREFIX_TRANSACTION_DONE) && messages.getSender().equals(clientId)) {
                    UUID transactionId = UUID.fromString(word.split(Constant.COLON)[1]);
                    letSenderKnowTransactionIsDone(transactionId);
                    this.serverData.addCompletedTransaction(transactionId);
                    this.serverData.removePendingTransaction(transactionId);
                    break;
                }
                else if(word.contains(Constant.PREFIX_TOURIST_TRANSACTION_DONE)) {
                    var touristTransaction = this.serverData.getAndRemovePendingTouristTransaction(UUID.fromString(word.split(Constant.COLON)[1]));
                    this.serverData.addCompletedTouristTransaction(touristTransaction);
                }
                else if(word.contains(Constant.PREFIX_TRANSACTION_DONE) && !messages.getSender().equals(clientId)) {
                    writeObject(messages);
                    break;
                }
                else if(word.contains(Constant.PREFIX_REVERT_TRANSACTION)) {
                    UUID transactionId = UUID.fromString(word.split(Constant.COLON)[1]);
                    letSenderKnowTransactionIsDone(transactionId);
                    this.serverData.addCompletedTransaction(transactionId);
                    this.serverData.removePendingTransaction(transactionId);
                    break;
                }
                else if (word.contains(Constant.PREFIX_TRANSACTION_FAILED)) {
                    revertIncompleteTransfer(splitMessage);
                    break;
                }
                else if(word.contains(Constant.COMMAND_ALL_CLIENTS_CONNECTED)) {
                    writeObject(messages);
                    break;
                }
                else if(word.contains(Constant.PREFIX_SERVER_ID)) {
                    writeObject(messages);
                    break;
                }
                else if(word.contains(Constant.COMMAND_SIMULATION_COMPLETED)) {
                    writeObject(messages);
                    break;
                }
                //todo: very prone if same client sends 2x, fix
                else if(word.contains(Constant.PREFIX_CLIENT_READY))
                    this.serverData.incrementReadyClients();
                else
                    this.isConnected = checkIfConnectionTerminated(word);
            }
        }
    }

    private void letSenderKnowTransactionIsDone(UUID transactionId) {
        var pendingTransfer = this.serverData.getPendingTransactionById(transactionId);
        addToClientMessageQueue(pendingTransfer.getClientIdFrom(),
                new Messages(Constant.PREFIX_TRANSACTION_DONE + transactionId, this.clientId));
    }

    /**
     * If a transfer does not go through to its recipient, the sender will be notified to be able to cancel the withdrawal
     * @param splitMessage
     * @throws InvalidKeyException if no transaction with the given ID is found, the exception is thrown
     */
    private void revertIncompleteTransfer(String[] splitMessage) throws InvalidKeyException {
        UUID transactionId = null;
        for(var word : splitMessage) {
            if(word.contains(Constant.PREFIX_TRANSACTION_ID))
                transactionId = UUID.fromString(word.split(Constant.COLON)[1]);
        }
        if(transactionId == null) throw new InvalidKeyException("Transaction ID not found");

        AccountTransaction accountTransaction = this.serverData.getPendingTransactionById(transactionId);
        addToClientMessageQueue(accountTransaction.getClientIdFrom(), new Messages(
                Constant.PREFIX_REVERT_TRANSACTION + transactionId, this.clientId
        ));
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
                this.serverData.getCurrentStep()) ||
                completedStep == this.serverData.getCurrentStep();
    }

    /**
     * Parse and validate the step being passed in
     * @param word
     */
    private void setSteps(String word)  {
        var completedStep = Integer.valueOf(word.split(Constant.COLON)[1]);

        if (validateSteps(completedStep))
            this.serverData.incrementStepOfConnectedSocketByClientId(this.clientId);
        else
            logger.logStepMismatchError(completedStep, this.serverData.getCurrentStep(), this.clientId.toString());
    }

    /**
     * This sets the ClientID of the socket that the Threadneedle program is connected to
     * @param word
     * @return
     */
    private boolean checkForConnectionId(String word) {
        if (word.contains(Constant.PREFIX_SET_CLIENT_ID)) {
            UUID clientId = UUID.fromString(word.split(Constant.COLON)[1]);
            setClientId(clientId);
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
