package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Main {

    public static void main(String[] args) {
        ServerData serverData;
        int stepNumber = 0;
        int connectionLimit = 0;
        int portNumber = 4242;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<>(Arrays.asList(args));

            //TODO: Make --help flag with info
            try {
                for (var i = 0; i < argumentsList.size(); i++) {
                    if (Objects.equals(argumentsList.get(i), Constants.FLAG_STEP_NUMBER))
                        stepNumber = Integer.parseInt(argumentsList.get(i + 1));

                    if (Objects.equals(argumentsList.get(i), Constants.FLAG_CONNECTION_NUMBER))
                        connectionLimit = Integer.parseInt(argumentsList.get(i + 1));

                    if (Objects.equals(argumentsList.get(i), Constants.FLAG_PORT_NUMBER))
                        portNumber = Integer.parseInt(argumentsList.get(i + 1));
                }
            } catch (Exception e) {
                logger.logInvalidArgumentError();
                System.exit(0);
            }

            if(connectionLimit <= 0 || stepNumber < 1) {
                logger.logIncorrectArgumentsError(stepNumber, connectionLimit);
                System.exit(0);
            }

            logger.logArguments(stepNumber, connectionLimit);
            // needs to be final because of the new Thread() call, don't want the data to change...
            Integer finalStepNumber = stepNumber;
            Integer finalConnectionLimit = connectionLimit;
            serverData = new ServerData(finalStepNumber, finalConnectionLimit, portNumber);

            var serverThread = new Thread(() -> {
                var server = new Server();
                server.start(serverData);
            });

            serverThread.start();
        } else {
            logger.logInvalidArgumentError();
            System.exit(0);
        }

    }
}
