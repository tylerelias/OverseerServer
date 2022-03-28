package overseer;

public class Constant {
    // Program arguments
    static final String ARG_CONNECTION_NUMBER = "-c";
    static final String ARG_PORT_NUMBER = "-p";
    static final String ARG_DEBUG = "-d";
    // log related
    static final String ERROR_STEP_MISMATCH = "Step mismatch";
    static final String EXCEPTION_THROWN = "Exception";
    static final String ARGUMENT_WARNING = "Arguments";
    // Commands coming from/to server/sockets
    public static final String TERMINATE_CONNECTION = "terminate"; // connection termination request
    //prefixes:
    public static final String PREFIX_NEXT_STEP = "NextStep:";      // the next step that the Overseer tells the clients to take
    public static final String PREFIX_CURRENT_STEP = "CurrentStep:";// message will contain the current step that the client is at
    public static final String PREFIX_SERVER_ID = "ServerID:";      // The ID of the server is sent with this prefix
    public static final String PREFIX_SET_CLIENT_ID = "SetClientID:";// The Client ID gets set when Threadneedle sends this prefix to Overseer
    public static final String PREFIX_RECEIVED_CLIENT_ID = "ReceivedClientID:"; // Sends all connected Client ID's to connected clients
    public static final String PREFIX_TRANSACTION_ID = "TransactionId:"; // the bank transaction ID's get sent with the prefix
    public static final String PREFIX_TRANSACTION_DONE = "TransactionDone:"; // confirmation from Client when transaction is done
    public static final String PREFIX_TOURIST_TRANSACTION_DONE = "TouristTransactionDone:"; // confirmation from Client when Tourist transaction is done
    public static final String PREFIX_TRANSACTION_FAILED = "TransactionFailed:"; // failed transaction
    public static final String PREFIX_REVERT_TRANSACTION = "RevertTransaction:"; // transaction reverting successful
    // Send this msg + Client ID to Overseer when the client is ready
    public static final String PREFIX_CLIENT_READY = "ClientReady:";     // sent when client has loaded all their configs & settings
    public static final String PREFIX_TAKE_STEP = "TakeStep:";     // sent when client has loaded all their configs & settings
    // Command related
    public static final String COMMAND_ALL_CLIENTS_CONNECTED = "all_clients_connected:"; //sent from Overseer, to clients that all clients have connected
    public static final String COMMAND_SIMULATION_COMPLETED = "simulation_completed";   // sent from Overseer, to client that simulation is completed
    public static final String COMMAND_SPLITTER = ";";  // splits command with CMD1;CMD2
    public static final String COLON = ":";             // used for string splitting CMD1:DATA
}
