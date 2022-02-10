package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Main {


    public static void main(String[] args) {
        AtomicReference<ServerData> serverData = new AtomicReference<>();
        String stepNumber = null;
        int connectionLimit = 0;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<>(Arrays.asList(args));

            for (var i = 0; i < argumentsList.size(); i++) {
                if (Objects.equals(argumentsList.get(i), Constants.FLAG_STEP_NUMBER))
                    stepNumber = argumentsList.get(i + 1);

                if (Objects.equals(argumentsList.get(i), Constants.FLAG_CONNECTION_NUMBER))
                    connectionLimit = Integer.parseInt(argumentsList.get(i + 1));
            }
        }

        if(connectionLimit <= 0 || stepNumber == null) {
            logger.logIncorrectArgumentsError(stepNumber, connectionLimit);
            System.exit(0);
        }

        logger.logArguments(stepNumber, connectionLimit);

        Integer finalStepNumber = Integer.parseInt(stepNumber);
        Integer finalConnectionLimit = connectionLimit;

        var serverThread = new Thread(() -> {
            var server = new Server();
            serverData.set(new ServerData(server, finalStepNumber, finalConnectionLimit, 0));
            server.start(serverData);
        });

        serverThread.start();
    }
}
