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

            while(true) {
               if(serverData.get().checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    createConnectionThread();
                } else if (!isAtConnectionLimit){
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(serverData.get().getCurrentConnections());
                    sendClientsMessage();
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
        this.serverData.get().setCurrentConnections(this.serverData.get().getCurrentConnections() + 1);
    }

    private void sendClientsMessage() {
        var socketList = this.serverData.get().getConnectedSockets();
        socketList.forEach(s -> {
            try {
                var dataOutputStream = new DataOutputStream(s.getOutputStream());
                dataOutputStream.writeUTF("Simulation:all_clients_connected;Socket:" + s.hashCode());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void waitForConfirmationFromAllClients() {
        while (this.serverData.get().checkIfAllClientsConnected()) {

        }
    }


}
