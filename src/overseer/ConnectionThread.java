package overseer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.Objects;

public class ConnectionThread extends Thread {
    // Constants come here, mostly for messages from/to client
    static final String TERMINATE_CONNECTION = "terminate";
    static final String ABORT_CONNECTION = "abort";
    static final String WORD_STEP = "Step:";
    static final String WORD_CONNECTION = "Connection:";
    static final String COMMAND_SPLITTER = ";";
    static final String WORD_SPLITTER = ":";
    static final String STEP_MISMATCH_ERROR = "Step mismatch";
    static final String EXCEPTION_THROWN = "Exception";
    static final String NO_MESSAGE = "no_message";
    // Info needed to verify and communicate with client
    private final Socket socket;
    private final Logger logger;
    String serverSteps;
    String clientSteps;
    String clientConnectionId;

    public ConnectionThread(Socket clientSocket, String _steps) {
        this.socket = clientSocket;
        this.serverSteps = _steps;
        logger = new Logger();
    }

    public void run() {
        boolean isConnected = true;
        try {
            while (isConnected) {
                String message = readMessageFromSocket(this.socket);
                isConnected = checkIfConnectionTerminated(message);
            }
            closeSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket() throws IOException {
        this.socket.close();
        this.logger.log(String.format("%s: Socket gracefully closed", clientConnectionId));
    }

    private boolean checkIfConnectionTerminated(String message) {
        return !(message.equals(TERMINATE_CONNECTION) ||
                message.equals(ABORT_CONNECTION));
    }

    private String readMessageFromSocket(Socket socket) {
        try {
            var dataInputStream = new DataInputStream(socket.getInputStream());
            String message = dataInputStream.readUTF();

            // Todo: Remove print in future?
            this.logger.log(String.format("Message: %s. On thread: %s", message, Thread.currentThread()));

            processMessage(message);
            return message;
        } catch (EOFException e) {
            // This is not a problem because this simply means that
            // the socket had no message to send, so move along
        } catch (Exception e) {
            this.logger.logError(
                    String.format("Exception thrown in ConnectionThread: %s", e.getMessage()),
                    EXCEPTION_THROWN
            );
        }
        return NO_MESSAGE;
    }

    private void processMessage(String message) throws IOException {
        var splitMessage = message.split(COMMAND_SPLITTER);
        var isConnectionIdSet = false;
        var isStepsSet = false;

        for(var word : splitMessage) {
            // prevent unnecessary lookups if we already got the id
            if(!isConnectionIdSet) isConnectionIdSet = checkForConnectionId(word);
            // same as above, but for steps
            if(!isStepsSet) isStepsSet = checkForSteps(word);
        }
    }

    private boolean validateSteps() {
        return Objects.equals(clientSteps, serverSteps);
    }

    private boolean checkForSteps(String word) throws IOException {
        if (word.contains(WORD_STEP)) {

            var trimW = word.split(WORD_SPLITTER);
            setClientSteps(trimW[1]);
            // Make sure that the client has the same steps sets as the server
            // if that is not the case, the connection will be closed
            if(validateSteps())
                return true;
            else {
                socket.close();
                String errorMessage = String.format(
                        "Client: %s, Server: %s. Connection to client %s will be terminated.",
                        clientSteps, serverSteps, clientConnectionId);

                logger.logError(errorMessage, STEP_MISMATCH_ERROR);
                throw new InvalidParameterException(errorMessage);
            }
        }
        return false;
    }

    private boolean checkForConnectionId(String word) {
        if (word.contains(WORD_CONNECTION)) {
            var trimW = word.split(WORD_SPLITTER);
            setClientConnectionId(trimW[1]);
            return true;
        }
        return false;
    }

    private void setClientConnectionId(String connectionId){
        this.clientConnectionId = connectionId;
    }

    private void setClientSteps(String steps) {
        this.clientSteps = steps;
    }
}
