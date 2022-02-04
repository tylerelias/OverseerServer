package overseer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    //TODO: save this to a file

    public void log(String message) {
        System.out.printf("[%s] : [Log] %s%n", getTimeStamp(), message);
    }

    public void logError(String message, String errorType) {
        System.out.printf("[%s] : [ERROR - %s] %s%n", getTimeStamp(), errorType, message);
    }

    private String getTimeStamp() {
        return ZonedDateTime
                .now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss"));
    }
}
