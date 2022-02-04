package overseer;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static final Integer PORT = 4242;

    public void start(String _steps) {
        try {
            var serverSocket = new ServerSocket(PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new ConnectionThread(socket, _steps).start();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
