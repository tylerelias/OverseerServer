package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Main {

    public static void main(String[] args) {
        ServerData serverData;
        int connectionLimit = 0;
        int portNumber = 4242;
        boolean isDebugEnabled = false;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<>(Arrays.asList(args));

            //TODO: Make --help flag with info
            try {
                for (var i = 0; i < argumentsList.size(); i++) {
                    if (Objects.equals(argumentsList.get(i), Constants.ARG_CONNECTION_NUMBER))
                        connectionLimit = Integer.parseInt(argumentsList.get(i + 1));

                    if (Objects.equals(argumentsList.get(i), Constants.ARG_PORT_NUMBER))
                        portNumber = Integer.parseInt(argumentsList.get(i + 1));
                    if(Objects.equals(argumentsList.get(i), Constants.ARG_DEBUG))
                        isDebugEnabled = true;
                }
            } catch (Exception e) {
                logger.logInvalidArgumentError();
                System.exit(0);
            }

            if(connectionLimit <= 0) {
                logger.logIncorrectArgumentsError(connectionLimit);
                System.exit(0);
            }

            logger.logArguments(connectionLimit);
            // needs to be final because of the new Thread() call, don't want the data to change...
            Integer finalConnectionLimit = connectionLimit;
            serverData = new ServerData(finalConnectionLimit, portNumber, isDebugEnabled);

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
