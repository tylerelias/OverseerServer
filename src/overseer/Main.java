package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Main {


    public static void main(String[] args) {
        ServerData serverData;
        String stepNumber = null;
        int connectionLimit = 0;
        int portNumber = 4242;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<>(Arrays.asList(args));

            //TODO: Make --help flag with info

            for (var i = 0; i < argumentsList.size(); i++) {
                if (Objects.equals(argumentsList.get(i), Constants.FLAG_STEP_NUMBER))
                    stepNumber = argumentsList.get(i + 1);

                if (Objects.equals(argumentsList.get(i), Constants.FLAG_CONNECTION_NUMBER))
                    connectionLimit = Integer.parseInt(argumentsList.get(i + 1));

                if (Objects.equals(argumentsList.get(i), Constants.FLAG_PORT_NUMBER))
                    portNumber = Integer.parseInt(argumentsList.get(i + 1));
            }
        }

        if(connectionLimit <= 0 || stepNumber == null) {
            logger.logIncorrectArgumentsError(stepNumber, connectionLimit);
            System.exit(0);
        }

        logger.logArguments(stepNumber, connectionLimit);

        Integer finalStepNumber = Integer.parseInt(stepNumber);
        Integer finalConnectionLimit = connectionLimit;
        serverData = new ServerData(finalStepNumber, finalConnectionLimit, portNumber);

        var serverThread = new Thread(() -> {
            var server = new Server();
            server.start(serverData);
        });

        serverThread.start();
    }
}
