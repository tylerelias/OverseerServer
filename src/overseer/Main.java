package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static void main(String[] args) {
        AtomicReference<ServerData> serverData = new AtomicReference<>();
        var stepNumber = "";
        var connectionLimit = 0;
        var logger = new Logger();

        if (args.length > 0) {
            var argumentsList = new ArrayList<String>(Arrays.asList(args));

            for (var i = 0; i < argumentsList.size(); i++) {
                if (Objects.equals(argumentsList.get(i), "--sn"))
                    stepNumber = argumentsList.get(i + 1);

                if (Objects.equals(argumentsList.get(i), "--cn"))
                    connectionLimit = Integer.parseInt(argumentsList.get(i + 1));
            }
        }

        logger.log(String.format("Argument - Step No: %s", stepNumber));
        logger.log(String.format("Argument - Connection No: %s", connectionLimit));

        String finalStepNumber = stepNumber;
        Integer finalConnectionLimit = connectionLimit;
        var serverThread = new Thread(() -> {
            var server = new Server();
            serverData.set(new ServerData(server, finalStepNumber, finalConnectionLimit, 0));
            server.start(serverData);
        });

        serverThread.start();
        logger.log("Server thread is now up and waiting for connections");
    }
}
