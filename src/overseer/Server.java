package overseer;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static final Integer PORT = 4242;
    static String steps;

    public void start(String _steps) {
        try {
            var serverSocket = new ServerSocket(PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new ConnectionThread(socket, steps).start();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
