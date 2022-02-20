package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;

public class Server {
    private final Logger logger = new Logger();
    private ServerData serverData;
    private ServerSocket serverSocket;

    public void start(ServerData serverData) {
        this.serverData = serverData;

        try {
            this.serverSocket = new ServerSocket(this.serverData.getPortNumber());
            var isAtConnectionLimit = false;
            logServerInfo();

            while (!this.serverSocket.isClosed()) {
                // While the connected clients < the connection limit
                if (this.serverData.checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    createConnectionThread();
                }
                // When the connection limit has been reached
                else if (!isAtConnectionLimit) {
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(this.serverData.getCurrentConnections().get());
                }
                // Now simulation can begin
                if (validateSteppingConditions(isAtConnectionLimit)) {
                    // At the moment the only thing the Overseer will do is tell the clients
                    // to start proceeding the step (n+1) and wait for all clients to reach said step
                    incrementCurrentServerStep();
                    tellAllClientsToStep();
                    waitForAllClientsToCompleteSteps();
                }
                if(isSimulationCompleted()) {
                    tellAllClientsSimulationIsCompleted();
                    logger.logSimulationCompleted(this.serverData.getCurrentStep().get());
                    this.serverSocket.close();
                }
            }
        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void logServerInfo() {
        SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
        var ipAddress = socketAddress.toString().split("/")[1];
        logger.logServerInformation(ipAddress);
    }

    private boolean validateSteppingConditions(boolean isAtConnectionLimit) {
        return isAtConnectionLimit &&
                areAllConnectionThreadsAtSameStep() &&
                !isSimulationCompleted();
    }

    private boolean isSimulationCompleted() {
        return this.serverData.getCurrentStep().get() == this.serverData.getTotalSteps();
    }

    private void tellAllClientsToStep() {
        logger.logTellAllClientsToStep(this.serverData.getCurrentStep().get());
        sendAllClientsMessage(
            Constants.PREFIX_NEXT_STEP
            + (this.serverData.getCurrentStep().get())
        );
    }

    private void tellAllClientsSimulationIsCompleted() {
        sendAllClientsMessage(
            Constants.PREFIX_SIMULATION +
            Constants.COMMAND_SIMULATION_COMPLETED +
            Constants.COMMAND_SPLITTER +
            Constants.PREFIX_CONNECTION +
            Constants.TERMINATE_CONNECTION
        );
        this.serverData.closeAllSockets();
    }

    private boolean areAllConnectionThreadsAtSameStep() {
        for (ConnectedSockets s : this.serverData.getConnectedSockets().values()) {
            if (s.getCurrentStep().get() != this.serverData.getCurrentStep().get())
                return false;
        }
        return true;
    }

    private void createConnectionThread() throws IOException {
        Socket clientSocket = this.serverSocket.accept();

        new ConnectionThread(clientSocket, this.serverData).start();
        writeMessageToSocket(clientSocket,
                Constants.PREFIX_TOTAL_STEPS + this.serverData.getTotalSteps()
                );
        this.serverData.incrementCurrentConnections();
    }

    private void sendAllClientsMessage(String message) {
        var socketList = this.serverData.getConnectedSockets().values();
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
        var haveAllCompletedSteps = false;
        while (!haveAllCompletedSteps) {
            var completed = 0;
            for (ConnectedSockets connectedSocket : this.serverData.getConnectedSockets().values()) {
                if (Objects.equals(connectedSocket.getCurrentStep().get(), this.serverData.getCurrentStep().get()))
                    completed++;
                
                if (completed == this.serverData.getCurrentConnections().get()) {
                    haveAllCompletedSteps = true;
                    break;
                }
            }
        }
    }

    private void incrementCurrentServerStep() {
        this.serverData.incrementCurrentStep();
    }
}
