package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final String CONNECTION_NUMBER_FLAG = "--cn";
    private static final String STEP_NUMBER_FLAG = "--sn";

    public static void main(String[] args) {
        AtomicReference<ServerData> serverData = new AtomicReference<>();
        String stepNumber = null;
        int connectionLimit = 0;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<>(Arrays.asList(args));

            for (var i = 0; i < argumentsList.size(); i++) {
                if (Objects.equals(argumentsList.get(i), STEP_NUMBER_FLAG))
                    stepNumber = argumentsList.get(i + 1);

                if (Objects.equals(argumentsList.get(i), CONNECTION_NUMBER_FLAG))
                    connectionLimit = Integer.parseInt(argumentsList.get(i + 1));
            }
        }

        if(connectionLimit <= 0 || stepNumber == null) {
            logger.logIncorrectArgumentsWarning(stepNumber, connectionLimit);
            System.exit(0);
        }

        logger.logArguments(stepNumber, connectionLimit);

        String finalStepNumber = stepNumber;
        Integer finalConnectionLimit = connectionLimit;
        var serverThread = new Thread(() -> {
            var server = new Server();
            serverData.set(new ServerData(server, finalStepNumber, finalConnectionLimit, 0));
            server.start(serverData);
        });

        serverThread.start();
    }
}
