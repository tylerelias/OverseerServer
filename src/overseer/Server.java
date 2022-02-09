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
                            "Clients:all_clients_connected;Step:"
                                    + (this.serverData.get().getCurrentStep() + 1)
                    );
                    waitForResponseFromAllClients();
                }
            }

        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void createConnectionThread() throws IOException {
        Socket socket = this.serverSocket.accept();
        this.serverData.get().addSocket(socket);
        new ConnectionThread(socket, this.serverData).start();
        this.serverData.get().incrementCurrentConnections();
    }

    private void sendAllClientsMessage(String message) {
        var socketList = this.serverData.get().getConnectedSockets();
        socketList.forEach(s -> {
            try {
                if (!s.isClosed()) {
                    writeMessageToSocket(s, message);
                } else
                    logger.logSocketClosed(String.format("Socket %s is closed", s.hashCode()));
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

    private void waitForResponseFromAllClients() {
        while (this.serverData.get().checkIfAllClientsConnected()) {

        }
    }

    private void simulateSteps() {
        int stepNumber = Integer.parseInt(this.serverData.get().getStepNumber());
        var connectedSockets = this.serverData.get().getConnectedSockets();

        for(var i = 0; i < stepNumber; i++) {
            sendAllClientsMessage("Step:" + stepNumber + 1);
        }
    }
}
