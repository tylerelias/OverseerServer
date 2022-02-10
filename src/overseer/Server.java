package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class Server {
    private static final Integer PORT = 4242;
    private final Logger logger = new Logger();
    private AtomicReference<ServerData> serverData;
    private ServerSocket serverSocket;

    public void start(AtomicReference<ServerData> serverData) {
        this.serverData = serverData;

        try {
            this.serverSocket = new ServerSocket(PORT);
            var isAtConnectionLimit = false;

            while(!this.serverSocket.isClosed()) {
               if(serverData.get().checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    createConnectionThread();
                }
               else if(!isAtConnectionLimit) {
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(serverData.get().getCurrentConnections());
                    sendAllClientsMessage(
                            Constants.PREFIX_CLIENTS +
                            Constants.COMMAND_ALL_CLIENTS_CONNECTED +
                            Constants.COMMAND_SPLITTER +
                            Constants.PREFIX_NEXT_STEP
                            + (this.serverData.get().getCurrentStep() + 1)
                    );
                    waitForAllClientsToCompleteSteps();
                }
            }

        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void createConnectionThread() throws IOException {
        Socket socket = this.serverSocket.accept();
        this.serverData.get().addSocket(socket, socket.hashCode(), this.serverData.get().getCurrentStep());
        new ConnectionThread(socket, this.serverData).start();
        this.serverData.get().incrementCurrentConnections();
    }

    private void sendAllClientsMessage(String message) {
        var socketList = this.serverData.get().getConnectedSockets();
        socketList.forEach(s -> {
            try {
                var sSocket = s.getSocket();
                if (!sSocket.isClosed()) {
                    writeMessageToSocket(sSocket,
                            Constants.PREFIX_CLIENT_ID +
                            sSocket.hashCode() +
                            Constants.COMMAND_SPLITTER +
                            message);
                } else
                    logger.logSocketClosed(s.hashCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void writeMessageToSocket(Socket s, String message) throws IOException {
        var dataOutputStream = new DataOutputStream(s.getOutputStream());
        dataOutputStream.writeUTF(message);
        dataOutputStream.flush();
    }

    private void waitForAllClientsToCompleteSteps() {
        System.out.println("Waiting for responses...");

    }

    private void simulateSteps() {
        int stepNumber = Integer.parseInt(this.serverData.get().getStepNumber());
        var connectedSockets = this.serverData.get().getConnectedSockets();

        for(var i = 0; i < stepNumber; i++) {
            sendAllClientsMessage(Constants.PREFIX_NEXT_STEP + stepNumber + 1);
        }
    }
}
