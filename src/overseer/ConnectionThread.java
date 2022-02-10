package overseer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class ConnectionThread extends Thread {
    // Info needed to verify and communicate with client
    private final Socket socket;
    private final Logger logger;
    // There data in serverData is used between Server.java and the threads in ConnectionThread
    private final ServerData serverData;
    private Integer clientId;

    public ConnectionThread(Socket clientSocket, ServerData serverData) {
        this.socket = clientSocket;
        this.serverData = serverData;
        this.logger = new Logger();
        this.clientId = 0;
    }

    // Socket will keep reading/writing messages as long as the connection
    // to the server is alive
    public void run() {
        boolean isConnected = true;
        try {
            while (isConnected && !this.socket.isClosed()) {
                String message = readMessageFromSocket();
                isConnected = checkIfConnectionTerminated(message);
            }
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

        if(!this.serverData.removeSocketByClientId(this.clientId))
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
            // TODO: Remove print in future?
            this.logger.logSocketMessage(message, String.valueOf(this.socket.hashCode()));
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

    private void processMessage(String message) {
        var splitMessage = message.split(Constants.COMMAND_SPLITTER);
        var isConnectionIdSet = false;
        var isStepsSet = false;

        for (var word : splitMessage) {
            // prevent unnecessary lookups if we already got the id
            if (!isConnectionIdSet || this.clientId == 0) isConnectionIdSet = checkForConnectionId(word);
            // same as above, but for steps
            if (!isStepsSet) isStepsSet = checkForSteps(word);
        }
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
                var serverSteps = this.serverData
                        .getCurrentStep()
                        .get();
                logger.logStepMismatchError(completedStep, serverSteps, this.clientId);
            }
        }
        return false;
    }

    private void incrementClientStep() {
        this.serverData.incrementStepOfConnectedSocketByClientId(this.clientId);
    }

    private boolean checkForConnectionId(String word) {
        if (word.contains(Constants.PREFIX_CLIENT_ID)) {
            var splitWord = word.split(Constants.COLON);
            setClientId(Integer.valueOf(splitWord[1]));
            return true;
        }
        return false;
    }

    private void setClientId(Integer clientId) {
        this.clientId = clientId;
    }
}
