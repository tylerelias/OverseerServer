package overseer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Main {

    public static void main(String[] args) {
        var stepNumber = "";
        var connectionNumber = 0;

        if (args.length > 0) {
            var argumentsList = new ArrayList<String>(Arrays.asList(args));

            for (var i = 0; i < argumentsList.size(); i++) {
                if (Objects.equals(argumentsList.get(i), "--sn"))
                    stepNumber = argumentsList.get(i+1);

                if (Objects.equals(argumentsList.get(i), "--cn"))
                    connectionNumber = Integer.parseInt(argumentsList.get(i+1));
            }
        }

        System.out.println("Argument - Step No: " + stepNumber);
        System.out.println("Argument - Connection No: " + connectionNumber);

        String finalStepNumber = stepNumber;
        var serverThread = new Thread(() -> {
            var server = new Server();
            server.start(finalStepNumber);
        });

        serverThread.start();

        System.out.println("Server thread is now up and waiting for connections");
    }
}
