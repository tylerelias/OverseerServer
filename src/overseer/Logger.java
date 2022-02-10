package overseer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Logger {

    //TODO: save this to a file

    private void log(String message) {
        System.out.printf("[%s] : [Log] %s%n", getTimeStamp(), message);
    }

    private void logError(String message, String errorType) {
        System.out.printf("[%s] : [ERROR - %s] %s%n", getTimeStamp(), errorType, message);
    }

    private void logWarning(String message, String warningType) {
        System.out.printf("[%s] : [WARNING - %s] %s%n", getTimeStamp(), warningType, message);
    }

    private String getTimeStamp() {
        return ZonedDateTime
                .now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss"));
    }

    public void logCurrentConnections(Integer currentConnections) {
        log(String.format("Connected client(s): %s", currentConnections));
    }

    public void logServerError(Exception e) {
        logError(Arrays.toString(e.getStackTrace()), "Server");
    }

    public void logStepMismatchError(String clientSteps, String serverSteps, Integer clientConnectionId) {
        String errorMessage = String.format(
                "Client: %s, Server: %s. Connection to client %s will be terminated.",
                clientSteps, serverSteps, clientConnectionId);
        logError(errorMessage, Constants.ERROR_STEP_MISMATCH);
    }

    public void logConnectionThreadExceptionError(Exception e) {
        logError(String.format("In ConnectionThread: %s", e.getMessage()),
                Constants.EXCEPTION_THROWN);
        e.printStackTrace();
    }

    public void logSocketClosed(Integer clientConnectionId) {
        log(String.format("%s: Socket gracefully closed", clientConnectionId));
    }

    public void logSocketMessage(String message, String threadName) {
        log(String.format("Message: %s - %s", message, threadName));
    }

    public void logArguments(String stepNumber, int connectionLimit) {
        log(String.format("Argument - Total Steps: %s", stepNumber));
        log(String.format("Argument - Connection limit: %s", connectionLimit));
    }

    public void logIncorrectArgumentsError(String stepNumber, int connectionLimit) {
        logError(
                String.format("Arguments are not in the correct format: Connection limit: %s, Step number: %s",
                        connectionLimit, stepNumber),
                Constants.ARGUMENT_WARNING);
    }

    public void logConnectionLimitReached(Integer connectionLimit) {
        log(String.format("The connection limit of %s has been reached.", connectionLimit));
    }

    public void logErrorSocketNotInSocketList(Integer clientId) {
        logError(String.format("Socket %s was not found in the connected socket list", clientId),"ConnectionThread");
    }
}
