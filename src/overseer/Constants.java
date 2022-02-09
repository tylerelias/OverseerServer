package overseer;

public class Constants {
    // Program arguments
    static final String FLAG_CONNECTION_NUMBER = "--cn";
    static final String FLAG_STEP_NUMBER = "--sn";
    // log related
    static final String ERROR_STEP_MISMATCH = "Step mismatch";
    static final String EXCEPTION_THROWN = "Exception";
    static final String ARGUMENT_WARNING = "Arguments";
    // Commands coming from/to server/sockets
    static final String TERMINATE_CONNECTION = "terminate";
    static final String DISCONNECTED = "disconnected";
    static final String ABORT_CONNECTION = "abort";
    static final String NO_MESSAGE = "no_message";
    // Command related
    static final String WORD_STEP = "Step:";
    static final String WORD_CONNECTION = "Connection:";
    static final String COMMAND_SPLITTER = ";";
    static final String WORD_SPLITTER = ":";
}
