package overseer;

import java.io.IOException;
import java.net.ServerSocket;
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
            var hasInitializedSimulation = false;
            logServerInfo();

            while (!this.serverSocket.isClosed()) {
                // While the connected clients < the connection limit
                if (this.serverData.checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    acceptConnections();
                }
                // When the connection limit has been reached
                else if (!isAtConnectionLimit) {
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(this.serverData.getCurrentConnections().get());
                }
                // Send all connected required data to clients
                if(!hasInitializedSimulation && this.serverData.haveAllClientsBeenInitialized())
                {
                    sendClientsSimulationInformation();
                    hasInitializedSimulation = true;
                    this.serverData.setHasSimulationStarted(true);
                }
                // Now simulation can begin
                if (hasInitializedSimulation && isAtConnectionLimit && validateSteppingConditions()) {
                    incrementCurrentServerStep();
                    tellAllClientsToStep();
                    waitForAllClientsToCompleteSteps();
                }
                if (isSimulationCompleted()) {
                    tellAllClientsSimulationIsCompleted();
                    logger.logSimulationCompleted(this.serverData.getCurrentStep().get());
                    this.serverSocket.close();
                }
                Thread.sleep(1);
            }
        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    // Send all the required information needed for the Threadneedle client once
    // all clients have connected and all the basic data has been gathered
    private void sendClientsSimulationInformation() {
        var clientIds = this.serverData.convertConnectedClientIdToUUID();
        sendAllClientsMessage(
            Constants.COMMAND_ALL_CLIENTS_CONNECTED + Constants.COMMAND_SPLITTER +
            Constants.PREFIX_RECEIVED_CLIENT_ID + clientIds
        );
        sendAllClientsObject(this.serverData.getBankInformationHashMap());
    }

    private void logServerInfo() {
        SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
        var ipAddress = socketAddress.toString().split("/")[1];
        logger.logServerInformation(ipAddress);
    }

    private boolean validateSteppingConditions() throws InterruptedException {
        return areAllConnectionThreadsAtSameStep() &&
                !isSimulationCompleted() &&
                this.serverData.isPersonTransactionEmpty();
    }

    private boolean isSimulationCompleted() throws InterruptedException {
        if(this.serverData.getCurrentStep().get() == this.serverData.getTotalSteps()) {
            if(!this.serverData.isPersonTransactionEmpty())
                while (!this.serverData.isPersonTransactionEmpty()) { Thread.sleep(1); }
            return true;
        }
        return false;
    }

    private void tellAllClientsToStep() {
        logger.logTellAllClientsToStep(this.serverData.getCurrentStep().get());
        sendAllClientsMessage(
            Constants.PREFIX_NEXT_STEP
            + (this.serverData.getCurrentStep().get()));
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
            if (socket.getCurrentStep() != this.serverData.getCurrentStep().get())
                return false;
        }
        return true;
    }

    private void acceptConnections() throws IOException {
        var threadneedleSocket = this.serverSocket.accept();
        new ConnectionThread(threadneedleSocket, this.serverData).start();
        this.serverData.incrementCurrentConnections();
    }

    private void sendAllClientsObject(Object object) {
        var connectedSockets = this.serverData.getConnectedSockets().values();
        connectedSockets.forEach(socket -> {
            try {
                var connectedSocket = socket.getThreadneedleSocket();
                if (!connectedSocket.isClosed()) {
                    socket.addToMessageQueue(object);
                } else
                    logger.logSocketClosed(socket.getClientId().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendAllClientsMessage(String message) {
        var connectedSockets = this.serverData.getConnectedSockets().values();
        connectedSockets.forEach(socket -> {
            try {
                var connectedSocket = socket.getThreadneedleSocket();
                if (!connectedSocket.isClosed()) {
                    socket.addToMessageQueue(
                        new Messages(Constants.PREFIX_CLIENT_ID +
                        socket.getClientId() +
                        Constants.COMMAND_SPLITTER +
                        message));
                } else
                    logger.logSocketClosed(socket.getClientId().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void waitForAllClientsToCompleteSteps() throws InterruptedException {
        var haveAllCompletedSteps = false;

        while (!haveAllCompletedSteps) {
            if (areAllConnectionThreadsAtSameStep() && this.serverData.isPersonTransactionEmpty())
                haveAllCompletedSteps = true;
            Thread.sleep(1);
        }
    }

    private void incrementCurrentServerStep() {
        this.serverData.incrementCurrentStep();
    }
}
