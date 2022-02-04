package overseer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ConnectionThread extends Thread {
    // Constants come here, mostly for messages from/to client
    static final String TERMINATE_CONNECTION = "terminate";
    static final String WORD_STEP = "Step:";
    static final String WORD_CONNECTION = "Connection:";
    static final String COMMAND_SPLITTER = ";";
    static final String WORD_SPLITTER = ":";
    // Info needed to verify and communicate with client
    protected Socket socket;
    String serverSteps;
    String clientSteps;
    String clientConnectionId;

    public ConnectionThread(Socket clientSocket, String _steps) {
        this.socket = clientSocket;
        this.serverSteps = _steps;
    }

    public void run() {
        boolean isConnected = true;
        try {
            while (isConnected) {
                String message = readMessageFromSocket(this.socket);

                if (message.equals(TERMINATE_CONNECTION))
                    isConnected = false;
            }

            this.socket.close();
            System.out.println("Connection to socket gracefully terminated");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readMessageFromSocket(Socket socket) {
        try {
            var dataInputStream = new DataInputStream(socket.getInputStream());
            String message = dataInputStream.readUTF();

            // Todo: Remove print in future
            System.out.println("Message: " + message +
                    ". On thread: " + Thread.currentThread());

            processMessage(message);

            return message.toString();
        } catch (EOFException e) {
            // This is not a problem because this simply means that
            // the socket had no message to send, so move along
        } catch (Exception e) {
            System.out.println("Exception thrown in ConnectionThread: " + e.getMessage());
        }
        // fugly, change this later
        return "";
    }

    private void processMessage(String message) {
        var splitMessage = message.split(COMMAND_SPLITTER);
        var isConnectionIdSet = false;
        var isStepsSet = false;

        for(var word : splitMessage) {
            // prevent unnecessary lookups if we already got the id
            if(!isConnectionIdSet) isConnectionIdSet = checkForConnectionId(word);
            // same as above, but for seps
            if(!isStepsSet) isStepsSet = checkForSteps(word);
        }
    }

    private boolean checkForSteps(String word) {
        if (word.contains(WORD_STEP)) {
            var trimW = word.split(WORD_SPLITTER);
            setClientSteps(trimW[1]);
            return true;
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
