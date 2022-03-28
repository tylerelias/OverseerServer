package overseer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.UUID;

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
    private UUID serverId;
    private final Debug debug = new Debug();

    public void start(ServerData serverData) {
        this.serverData = serverData;
        this.serverId = UUID.randomUUID();

        if(this.serverData.isDebugEnabled())
            System.err.println("NOTE: Debug output is enabled! This will affect simulation speeds because of increased text output in console");

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
                    logger.logConnectionLimitReached(this.serverData.getCurrentConnections());
                }
                // Send all simulation required data to clients
                if(!hasInitializedSimulation && this.serverData.haveAllClientsBeenInitialized())
                {
                    sendClientsSimulationInformation();
                    hasInitializedSimulation = true;
                    this.serverData.setHasSimulationStarted(true); // client sockets use this in ConnectionThread
                }
                // Now simulation can begin
                if (hasInitializedSimulation && isAtConnectionLimit) {
                    if(this.serverData.getTotalSteps() == this.serverData.getCurrentStep())
                        readStepInputFromCommandLine();

                    if(validateSteppingConditions()) {
                        incrementCurrentServerStep();
                        tellAllClientsToStep();
                        waitForAllClientsToCompleteSteps();
                    }
                }
                Thread.sleep(1); // CPU usage was going through the roof, so busy-waiting it is
            }
        } catch (Exception e) {
            logger.logServerError(e);
        }
    }

    private void readStepInputFromCommandLine() throws IOException {
        while(true) {
            System.out.println("======================================================================");
            System.out.println("> Set the number of steps you want the clients to take and hit (Enter)");
            var input = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if(input != null) {
                boolean isNumeric = input.chars().allMatch(Character::isDigit);
                if(isNumeric && Integer.parseInt(input) > 0) {
                    var value = Integer.parseInt(input);
                    commandClientsToTakeStep(value);
                    this.serverData.setTotalSteps(value);
                    break;
                }
            }
            System.out.println("Invalid input. Only positive numbers are accepted");
        }
    }

    private void commandClientsToTakeStep(int stepAmount) {
        sendAllClientsObject(new Messages(Constant.PREFIX_TAKE_STEP + stepAmount, this.serverId));
    }

    /**
     * Send all the required information needed for the Threadneedle client once
     * all clients have connected and all the basic data has been gathered
     */
    private void sendClientsSimulationInformation() {
        var clientIds = this.serverData.convertConnectedClientIdToUUID();
        sendAllClientsObject(new Messages(Constant.PREFIX_SERVER_ID + this.serverId, this.serverId));
        sendAllClientsObject(new Messages(Constant.COMMAND_ALL_CLIENTS_CONNECTED + clientIds, serverId));
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
                this.serverData.isPendingTransactionEmpty();
    }

    /**
     * Checks all the required conditions for the completion of the simulation
     * @return true of the simulation is finished, false if not
     */
    private boolean isSimulationCompleted() throws InterruptedException {
        if(this.serverData.getCurrentStep() == this.serverData.getTotalSteps()) {
            if(!this.serverData.isPendingTransactionEmpty())
                while (!this.serverData.isPendingTransactionEmpty()) { Thread.sleep(1); }
            return true;
        }
        return false;
    }

    /**
     * Sends a message to all client to commence the next step
     */
    private void tellAllClientsToStep() {
        logger.logTellAllClientsToStep(this.serverData.getCurrentStep());
        sendAllClientsObject(new Messages(
            Constant.PREFIX_NEXT_STEP
            + (this.serverData.getCurrentStep()), this.serverId)
        );
    }

    /**
     * Sends a message to all clients that the simulation as completed and terminate connection message as well
     */
    private void tellAllClientsSimulationIsCompleted() {
        if(this.serverData.isDebugEnabled())
            debug.serverTellAllClientsSimulationIsCompleted();

        sendAllClientsObject(new Messages(
                Constant.COMMAND_SIMULATION_COMPLETED +
                Constant.COMMAND_SPLITTER +
                Constant.TERMINATE_CONNECTION
        , serverId));
        this.serverData.closeAllSockets();
    }

    /**
     * Check to see if the connected clients have all reached the same step
     * @return true if they have all reached the same step, false if not
     */
    private boolean areAllConnectionThreadsAtSameStep() {
        if (!this.serverData.getCurrentConnections().equals(this.serverData.getConnectionLimit()))
            return false;

        for (ConnectedSocket socket : this.serverData.getConnectedSockets().values()) {
            if (socket.getCurrentStep() != this.serverData.getCurrentStep())
                return false;
        }
        return true;
    }

    /**
     * Accepts incoming Threadneedle connections and spawns a new ConnectionThread as a result
     */
    private void acceptConnections() {
        if(this.serverData.isDebugEnabled())
            debug.serverAcceptConnections(this.serverData.getCurrentConnections(), this.serverData.getConnectionLimit());

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
        if(this.serverData.isDebugEnabled())
            debug.serverSendAllClientsObject(object);

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
     *  A function that waits until all clients have completed their steps and if all pending transactions have been clear
     */
    private void waitForAllClientsToCompleteSteps() throws InterruptedException {
        var haveAllCompletedSteps = false;

        while (!haveAllCompletedSteps) {
            if (areAllConnectionThreadsAtSameStep() && this.serverData.isPendingTransactionEmpty())
                haveAllCompletedSteps = true;
            Thread.sleep(1);
        }
    }

    private void incrementCurrentServerStep() {
        this.serverData.incrementCurrentStep();
    }
}
