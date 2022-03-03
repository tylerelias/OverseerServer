package overseer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;

/**
 * The server accepts the set amount of connections of clients. It validates that the data and conditions are met to
 * proceed with the simulations for the connected clients. All clients connect to the Server but their socket
 * communication is handled in the ConnectionThread class.
 *
 * @since 2022
 */
public class Server {
    private final Logger logger = new Logger();
    private ServerData serverData; // where all the important server data is stored, sockets access it too
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
                // Send all simulation required data to clients
                if(!hasInitializedSimulation && this.serverData.haveAllClientsBeenInitialized())
                {
                    sendClientsSimulationInformation();
                    hasInitializedSimulation = true;
                    this.serverData.setHasSimulationStarted(true); // client sockets use this in ConnectionThread
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
                Thread.sleep(1); // CPU usage was going through the roof, so busy-waiting it is
            }
        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    /**
     * Send all the required information needed for the Threadneedle client once
     * all clients have connected and all the basic data has been gathered
     */
    private void sendClientsSimulationInformation() {
        var clientIds = this.serverData.convertConnectedClientIdToUUID();
        sendAllClientsMessage(
            Constants.COMMAND_ALL_CLIENTS_CONNECTED + Constants.COMMAND_SPLITTER +
            Constants.PREFIX_RECEIVED_CLIENT_ID + clientIds
        );
        sendAllClientsObject(this.serverData.getBankInformationHashMap());
    }

    /**
     * Used to display the IP address of the server itself. Makes it easier to fet the connection
     * information required for Threadneedle client when connecting
     */
    private void logServerInfo() {
        SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
        var ipAddress = socketAddress.toString().split("/")[1];
        logger.logServerInformation(ipAddress);
    }

    /**
     * All the required conditions to be able to proceed to the next step need to be met.
     * This function runs all those checks and returns the answer as a boolean
     * @return true if conditions are valid, false if not
     */
    private boolean validateSteppingConditions() throws InterruptedException {
        return areAllConnectionThreadsAtSameStep() &&
                !isSimulationCompleted() &&
                this.serverData.isPersonTransactionEmpty();
    }

    /**
     * Checks all the required conditions for the completion of the simulation
     * @return true of the simulation is finished, false if not
     */
    private boolean isSimulationCompleted() throws InterruptedException {
        if(this.serverData.getCurrentStep().get() == this.serverData.getTotalSteps()) {
            if(!this.serverData.isPersonTransactionEmpty())
                while (!this.serverData.isPersonTransactionEmpty()) { Thread.sleep(1); }
            return true;
        }
        return false;
    }

    /**
     * Sends a message to all client to commence the next step
     */
    private void tellAllClientsToStep() {
        logger.logTellAllClientsToStep(this.serverData.getCurrentStep().get());
        sendAllClientsMessage(
            Constants.PREFIX_NEXT_STEP
            + (this.serverData.getCurrentStep().get()));
    }

    /**
     * Sends a message to all clients that the simulation as completed and terminate connection message as well
     */
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

    /**
     * Check to see if the connected clients have all reached the same step
     * @return true if they have all reached the same step, false if not
     */
    private boolean areAllConnectionThreadsAtSameStep() {
        if (this.serverData.getCurrentConnections().get() != this.serverData.getConnectionLimit())
            return false;

        for (ConnectedSockets socket : this.serverData.getConnectedSockets().values()) {
            if (socket.getCurrentStep() != this.serverData.getCurrentStep().get())
                return false;
        }
        return true;
    }

    /**
     * Accepts incoming Threadneedle connections and spawns a new ConnectionThread as a result
     */
    private void acceptConnections() {
        try {
            var threadneedleSocket = this.serverSocket.accept();
            new ConnectionThread(threadneedleSocket, this.serverData).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.serverData.incrementCurrentConnections();
    }

    /**
     * Sends an object to all the connected clients
     * @param object that is to be sent to all clients
     */
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

    /**
     * Sends a message to all clients in the simulation. Note: Since this basically just wraps the String message
     * into a Messages object, one could just refactor the code to all sendAllClientsObject(Object object) function
     * in the future
     * @param message the message that is going to be sent to all the connected clients
     */
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

    /**
     *  A function that waits until all clients have completed their steps and if all pending transactions have been clear
     */
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
