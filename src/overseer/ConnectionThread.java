package overseer;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;

public class ConnectionThread extends Thread {
    // Info needed to verify and communicate with client
    private final Socket socket;
    private final Logger logger;
    // There data in serverData is used between Server.java and the threads in ConnectionThread
    private final ServerData serverData;
    private UUID clientId = null;
    //
    private boolean isConnectionIdSet;
    private boolean isTotalStepsCompared;

    public ConnectionThread(Socket clientSocket, ServerData serverData) {
        this.socket = clientSocket;
        this.serverData = serverData;
        this.logger = new Logger();
        this.isConnectionIdSet = false;
        this.isTotalStepsCompared = false;
    }

    // Socket will keep reading/writing messages as long as the connection
    // to the server is alive
    public void run() {
        boolean isConnected = true;
        try {
            while (isConnected && !this.socket.isClosed()) {
                String message = readMessageFromSocket();
                //TODO: Temp
//                logger.logSocketMessage(message, clientId.toString());
                isConnected = checkIfConnectionTerminated(message);
            }
            if(this.socket.isConnected())
                closeSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Close the socket and remove it from the serverData's currently connected sockets
    private void closeSocket() throws IOException {
        this.socket.close();
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

    private String readMessageFromSocket() throws IOException {
        try {
            String message = getDataInputStream();
            processMessage(message);
            return message;
        } catch (EOFException e) {
            // This is not a problem because this simply means that
            // the socket had no message to send, so move along
        } catch (Exception e) {
            logger.logConnectionThreadExceptionError(e);
            return Constants.TERMINATE_CONNECTION;
        }
        this.socket.close();
        return Constants.NO_MESSAGE;
    }

    private String getDataInputStream() throws IOException {
        var dataInputStream = new DataInputStream(socket.getInputStream());
        return dataInputStream.readUTF();
    }

    private void processMessage(String message) throws IOException {
        var splitMessage = message.split(Constants.COMMAND_SPLITTER);
        var isStepsSet = false;
        for (var word : splitMessage) {
            if (!isValidClientId())
                this.isConnectionIdSet = checkForConnectionId(word);
            if (!isStepsSet)
                isStepsSet = checkForSteps(word);
            if (isStepsSet && !this.isTotalStepsCompared)
                this.isTotalStepsCompared = confirmTotalSteps(word);
            if(word.contains(Constants.PREFIX_PERSON_OBJECT))
                readPersonObject();
            if(word.contains(Constants.PREFIX_BANK_OBJECT))
                readBankObject();
            if(word.contains(Constants.PREFIX_DEPOSIT_TO))
                readDepositRequest(message);
        }
    }

    private void readDepositRequest(String message) throws IOException {
        String[] splitMessage = message.split(Constants.COMMAND_SPLITTER);
        String personName = null;
        String bankName = null;
        UUID clientTo = null;
        UUID clientFrom = null;
        long amount = -1;
        int currentStep = -1;

        for(var word : splitMessage) {
            if(word.contains(Constants.PREFIX_PERSON_NAME))
                personName = word.split(Constants.COLON)[1];
            if(word.contains(Constants.PREFIX_BANK_NAME))
                bankName = word.split(Constants.COLON)[1];
            if(word.contains(Constants.PREFIX_CLIENT_TO))
                clientTo = UUID.fromString(word.split(Constants.COLON)[1]);
            if(word.contains(Constants.PREFIX_AMOUNT))
                amount = Long.parseLong(word.split(Constants.COLON)[1]);
            if(word.contains(Constants.PREFIX_CLIENT_ID))
                clientFrom = UUID.fromString(word.split(Constants.COLON)[1]);
            if(word.contains(Constants.PREFIX_TRANSFER_AT_STEP))
                currentStep = Integer.parseInt(word.split(Constants.COLON)[1]);
        }

        if(isDepositDataValid(clientTo, personName, bankName, amount, currentStep, clientFrom)) {
            sendDataOutputStream(message, clientTo);
            logger.logDepositTo(clientTo.toString(), personName, bankName, amount, currentStep);
        }
        System.out.println("Invalid deposit data");
        //todo: logError if validation fails
    }



    private void sendDataOutputStream(String message, UUID clientTo) throws IOException {
        var clientToSocket = this.serverData.getConnectedSockets().get(clientTo);
        var outputStream = clientToSocket.getSocket().getOutputStream();
        var dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeUTF(message);
        dataOutputStream.flush();
    }

    private boolean isDepositDataValid(UUID clientTo, String personName, String bankName, long amount, int currentStep, UUID clientFrom) {
        return clientTo != null &&
                personName != null &&
                bankName != null &&
                amount > 0 && currentStep > 0 &&
                clientFrom != null &&
                clientFrom.equals(this.clientId);
    }

    private void readPersonObject() {
        try {
            var objectInputStream = new ObjectInputStream(this.socket.getInputStream());
            var personInformation = (PersonInformation) objectInputStream.readObject();
            this.serverData.addClientInformation(this.clientId.toString(), personInformation);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void readBankObject() {
        try {
            var objectInputStream = new ObjectInputStream(this.socket.getInputStream());
            var bankInformation = (BankInformation) objectInputStream.readObject();
            this.serverData.addBankInformation(this.clientId, bankInformation);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidClientId() {
        return this.isConnectionIdSet && this.clientId != null;
    }

    private boolean confirmTotalSteps(String word) {
        if(word.contains(Constants.PREFIX_TOTAL_STEPS)) {
            var totalClientSteps = Integer.valueOf(word.split(Constants.COLON)[1]);
            // If for some reason the client and server don't have matching steps
            if(!totalClientSteps.equals(this.serverData.getTotalSteps())) {
                logger.logStepMismatchError(totalClientSteps, this.serverData.getTotalSteps());
                throw new IllegalArgumentException();
            }
            return true;
        }
        return false;
    }

    private boolean validateSteps(Integer completedStep) {
        return  Objects.equals(this.serverData.getConnectedSockedStepByClientId(this.clientId),
                this.serverData.getCurrentStep().get()) ||
                completedStep == this.serverData.getCurrentStep().get();
    }

    private boolean checkForSteps(String word)  {
        if (word.contains(Constants.PREFIX_CURRENT_STEP)) {
            var completedStep = Integer.valueOf(word.split(Constants.COLON)[1]);

            if (validateSteps(completedStep)) {
                incrementClientStep();
                return true;
            }
            else {
                var serverSteps = this.serverData.getCurrentStep().get();
                logger.logStepMismatchError(completedStep, serverSteps, this.clientId.toString());
            }
        }
        return false;
    }

    private void incrementClientStep() {
        this.serverData.incrementStepOfConnectedSocketByClientId(this.clientId);
    }

    private boolean checkForConnectionId(String word) throws IOException {
        if (word.contains(Constants.PREFIX_SET_CLIENT_ID)) {
            UUID clientId = UUID.fromString(word.split(Constants.COLON)[1]);
            setClientId(clientId);
            sendDataOutputStream(Constants.PREFIX_RECEIVED_CLIENT_ID + this.clientId);
            this.serverData.addSocket(this.socket, this.clientId);
            return true;
        }
        return false;
    }

    private void sendDataOutputStream(String message) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
        dataOutputStream.writeUTF(message);
        dataOutputStream.flush();
    }

    private void setClientId(UUID clientId) {
        this.clientId = clientId;
        logger.logClientIdSet(this.clientId.toString());
    }
}
