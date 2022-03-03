package overseer;

import java.io.Serializable;

public class Messages implements Serializable {
    private final String message;

    public Messages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
