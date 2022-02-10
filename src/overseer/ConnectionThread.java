package overseer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionThread extends Thread {
    // Info needed to verify and communicate with client
    private final Socket socket;
    private final Logger logger;
    private final AtomicReference<ServerData> serverData;
    Integer clientSteps;
    Integer clientId;

    public ConnectionThread(Socket clientSocket, AtomicReference<ServerData> serverData) {
        this.socket = clientSocket;
        this.serverData = serverData;
        this.logger = new Logger();
        this.clientSteps = 1;
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
                .get()
                .decrementCurrentConnections();

        if(!this.serverData.get().removeSocketByClientId(this.clientId))
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
            if (!isConnectionIdSet) isConnectionIdSet = checkForConnectionId(word);
            // same as above, but for steps
            if (!isStepsSet) isStepsSet = checkForSteps(word);
            //TODO: Steps
        }
    }

    private boolean validateSteps(Integer nextStep) {
        return Objects.equals(nextStep, this.serverData.get().getCurrentStep() + 1);
    }

    private boolean checkForSteps(String word) {
        if (word.contains(Constants.PREFIX_CURRENT_STEP)) {
            var nextStep = word.split(Constants.COLON)[1];

            if (validateSteps(Integer.valueOf(nextStep))) {
                this.clientSteps = this.serverData
                        .get()
                        .incrementStepOfConnectedSocketByClientId(this.clientId);
                return true;
            }
            else {
                var serverSteps = this.serverData.get().getCurrentStep();
                logger.logStepMismatchError(clientSteps, serverSteps, this.clientId);
                throw new InvalidParameterException();
            }
        }
        return false;
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
