package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

            while(true) {
               if(serverData.get().checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    createConnectionThread();
                } else if (!isAtConnectionLimit){
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(serverData.get().getCurrentConnections());
                    sendAllClientsMessage("Simulation:all_clients_connected");
                    waitForConfirmationFromAllClients();
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

    private void sendAllClientsMessage(String message) throws InterruptedException {
        System.out.println("Sending message to clients");
        var socketList = this.serverData.get().getConnectedSockets();
        socketList.forEach(s -> {
            try {
                if (!s.isClosed()) {
                    var dataOutputStream = new DataOutputStream(s.getOutputStream());
                    dataOutputStream.writeUTF(message);
                    dataOutputStream.flush();
                } else
                    logger.logSocketClosed(String.format("Socket %s is closed", s.hashCode()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void waitForConfirmationFromAllClients() {
        while (this.serverData.get().checkIfAllClientsConnected()) {

        }
    }

    private void simulateSteps() throws InterruptedException {
        int stepNumber = Integer.parseInt(this.serverData.get().getStepNumber());
        var connectedSockets = this.serverData.get().getConnectedSockets();

        for(var i = 0; i < stepNumber; i++) {
            sendAllClientsMessage("Step:" + stepNumber + 1);
        }
    }
}
