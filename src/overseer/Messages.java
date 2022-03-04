package overseer;

import java.io.Serializable;
import java.util.UUID;

public class Messages implements Serializable {
    private final String message;
    private final UUID sender;

    public Messages(String message, UUID sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public UUID getSender() {
        return sender;
    }
}
