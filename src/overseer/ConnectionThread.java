package overseer;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class ConnectionThread extends Thread {
    // Info needed to verify and communicate with client
    private final Socket socket;
    private final Logger logger;
    // There data in serverData is used between Server.java and the threads in ConnectionThread
    private final ServerData serverData;
    private Integer clientId;
    //
    private boolean isConnectionIdSet;
    private boolean isTotalStepsCompared;

    public ConnectionThread(Socket clientSocket, ServerData serverData) {
        this.socket = clientSocket;
        this.serverData = serverData;
        this.logger = new Logger();
        this.clientId = 0;
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
                logger.logSocketMessage(message, clientId.toString());
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
        this.serverData
                .decrementCurrentConnections();

        if(this.serverData.removeSocketByClientId(this.clientId) == null)
            logger.logErrorSocketNotInSocketList(this.clientId);

        this.logger.logSocketClosed(this.clientId);
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
            if(word.contains(Constants.COMMAND_PERSON))
                readPersonObject();
            if(word.contains(Constants.COMMAND_BANK))
                readBankObject();
        }
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
            this.serverData.addBankInformation(this.clientId.toString(), bankInformation);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidClientId() {
        return this.isConnectionIdSet && this.clientId != 0;
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
                logger.logStepMismatchError(completedStep, serverSteps, this.clientId);
            }
        }
        return false;
    }

    private void incrementClientStep() {
        this.serverData.incrementStepOfConnectedSocketByClientId(this.clientId);
    }

    private boolean checkForConnectionId(String word) throws IOException {
        if (word.contains(Constants.PREFIX_SET_CLIENT_ID)) {
            var clientId = word.split(Constants.COLON)[1];
            setClientId(Integer.parseInt(clientId));
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

    private void setClientId(Integer clientId) {
        this.clientId = clientId;
        logger.logClientIdSet(this.clientId.toString());
    }
}
