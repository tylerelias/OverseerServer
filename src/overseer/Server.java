package overseer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

            while (!this.serverSocket.isClosed()) {
                if (serverData.checkIfAllClientsConnected()) {
                    isAtConnectionLimit = false;
                    createConnectionThread();
                } else if (!isAtConnectionLimit) {
                    isAtConnectionLimit = true;
                    logger.logConnectionLimitReached(serverData.getCurrentConnections().get());
                }
                if (validateSteppingConditions(isAtConnectionLimit)) {
                    // At the moment the only thing the Overseer will do is tell the clients
                    // to start proceeding the step (n+1) and wait for all clients to reach said step
                    incrementCurrentServerStep();
                    tellAllClientsToStep();
                    //todo: only call once a simulationBegin = true has been made
                    waitForAllClientsToCompleteSteps();
                }
                if(isSimulationCompleted()) {
                    //TODO: Send clients that simulation is completed
                    logger.logSimulationCompleted(this.serverData.getCurrentStep().get());
                    this.serverSocket.close();
                }
            }

        } catch (Exception e) {
            logger.logServerError(e);
        }
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
                Constants.PREFIX_CLIENTS +
                        Constants.COMMAND_ALL_CLIENTS_CONNECTED +
                        Constants.COMMAND_SPLITTER +
                        Constants.PREFIX_NEXT_STEP
                        + (this.serverData.getCurrentStep().get())
        );
    }

    private boolean areAllConnectionThreadsAtSameStep() {
        for (ConnectedSockets s : this.serverData.getConnectedSockets()) {
            if (s.getCurrentStep().get() != this.serverData.getCurrentStep().get())
                return false;
        }
        return true;
    }

    private void createConnectionThread() throws IOException {
        Socket socket = this.serverSocket.accept();
        this.serverData.addSocket(socket, socket.hashCode(), this.serverData.getCurrentStep().get());
        new ConnectionThread(socket, this.serverData).start();
        this.serverData.incrementCurrentConnections();
    }

    private void sendAllClientsMessage(String message) {
        var socketList = this.serverData.getConnectedSockets();
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
            for (ConnectedSockets connectedSocket : this.serverData.getConnectedSockets()) {
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
