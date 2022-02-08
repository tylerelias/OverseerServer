package overseer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class Server {
    private static final Integer PORT = 4242;
    private final Logger logger = new Logger();


    public void start(AtomicReference<ServerData> serverData) {
        try {
            var serverSocket = new ServerSocket(PORT);

            while(true) {
                // keep an eye on this. Not a 100% sure but the check for the connectionLimit needs
                // to be currentConnections < connectionLimit, not <=...
                // could be that the last pass into the check blocks and waits on serverSocket.accept()?
                if(serverData.get().getCurrentConnections() < serverData.get().getConnectionLimit()) {
                    Socket socket = serverSocket.accept();
                    new ConnectionThread(socket, serverData).start();
                    serverData.get().setCurrentConnections(serverData.get().getCurrentConnections() + 1);
                } else {
                    System.out.println("Connection limit reached!");
                    Thread.sleep(1000);
                }
            }

        } catch (Exception e) {
            logger.logServerError(e);
        }
    }
}
