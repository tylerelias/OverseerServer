package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class Server {
    private final Logger logger = new Logger();
    private ServerData serverData;
    private ServerSocket serverSocket;

    public void start(ServerData serverData) {
        this.serverData = serverData;

        try {
            this.serverSocket = new ServerSocket(this.serverData.getPortNumber());
            var isAtConnectionLimit = false;
            var hasSentClientIds = false;
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
                // Send all connected client ID's to clients
                if(!hasSentClientIds && this.serverData.haveAllSocketsGottenAClientId())
                {
                    sendClientsSimulationInformation();
                    hasSentClientIds = true;
                }
                // Now simulation can begin
                else if (validateSteppingConditions() && isAtConnectionLimit && hasSentClientIds) {
                    // At the moment the only thing the Overseer will do is tell the clients
                    // to start proceeding the step (n+1) and wait for all clients to reach said step
                    incrementCurrentServerStep();
                    tellAllClientsToStep();
                    waitForAllClientsToCompleteSteps();
                }
                if (isSimulationCompleted()) {
                    tellAllClientsSimulationIsCompleted();
                    logger.logSimulationCompleted(this.serverData.getCurrentStep().get());
                    this.serverSocket.close();
                }
            }
        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void sendClientsSimulationInformation() {
        var clientIds = this.serverData.convertConnectedClientIdToString();
        sendAllClientsMessage(
            Constants.COMMAND_ALL_CLIENTS_CONNECTED + Constants.COMMAND_SPLITTER +
            Constants.PREFIX_RECEIVED_CLIENT_ID + clientIds
        );
    }

    private void logServerInfo() {
        SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
        var ipAddress = socketAddress.toString().split("/")[1];
        logger.logServerInformation(ipAddress);
    }

    private boolean validateSteppingConditions() {
        return areAllConnectionThreadsAtSameStep() && !isSimulationCompleted();
    }

    private boolean isSimulationCompleted() {
        return this.serverData.getCurrentStep().get() == this.serverData.getTotalSteps() &&
                areAllConnectionThreadsAtSameStep();
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

        if (this.serverData.getCurrentConnections().get() != this.serverData.getConnectionLimit())
            return false;

        for (ConnectedSockets socket : this.serverData.getConnectedSockets().values()) {
            if (socket.getCurrentStep().get() != this.serverData.getCurrentStep().get())
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
        var connectedSockets = this.serverData.getConnectedSockets().values();
        connectedSockets.forEach(socket -> {
            try {
                var sSocket = socket.getSocket();
                if (!sSocket.isClosed()) {
                    writeMessageToSocket(sSocket,
                Constants.PREFIX_CLIENT_ID +
                        socket.getClientId() +
                        Constants.COMMAND_SPLITTER +
                        message);
                } else
                    logger.logSocketClosed(socket.getClientId().toString());
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
            if (areAllConnectionThreadsAtSameStep())
                haveAllCompletedSteps = true;
        }
    }

    private void incrementCurrentServerStep() {
        this.serverData.incrementCurrentStep();
    }
}
