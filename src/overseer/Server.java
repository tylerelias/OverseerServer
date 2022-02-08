package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Server {
    private static final Integer PORT = 4242;
    private final Logger logger = new Logger();
    private final ArrayList<Socket> sockets = new ArrayList<>();


    public void start(AtomicReference<ServerData> serverData) {
        try {
            var serverSocket = new ServerSocket(PORT);
            var isAtConnectionLimit = false;

            while(true) {
                // keep an eye on this. Not a 100% sure but the check for the connectionLimit needs
                // to be currentConnections < connectionLimit, not <=...
                // could be that the last pass into the check blocks and waits on serverSocket.accept()?
                if(serverData.get().getCurrentConnections() < serverData.get().getConnectionLimit()) {
                    isAtConnectionLimit = false;
                    Socket socket = serverSocket.accept();
                    sockets.add(socket);
                    new ConnectionThread(socket, serverData).start();
                    serverData.get().setCurrentConnections(serverData.get().getCurrentConnections() + 1);
                } else if (!isAtConnectionLimit){
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(serverData.get().getCurrentConnections());
                    sendClientsMessage();
                    // 1.send response to sockets that simulation can begin
                    // 2.wait for confirmation that all agents are ready
                }
            }

        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void sendClientsMessage() {
        sockets.forEach((s) -> {
            try {
                var dataOutputStream = new DataOutputStream(s.getOutputStream());
                dataOutputStream.writeUTF("Sending every client this message! : " + s.hashCode());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
