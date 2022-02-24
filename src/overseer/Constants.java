package overseer;

public class Constants {
    // Program arguments
    static final String FLAG_CONNECTION_NUMBER = "-c";
    static final String FLAG_STEP_NUMBER = "-s";
    static final String FLAG_PORT_NUMBER = "-p";
    // log related
    static final String ERROR_STEP_MISMATCH = "Step mismatch";
    static final String EXCEPTION_THROWN = "Exception";
    static final String ARGUMENT_WARNING = "Arguments";
    // Commands coming from/to server/sockets
    static final String TERMINATE_CONNECTION = "terminate";
    static final String DISCONNECTED = "disconnected";
    static final String ABORT_CONNECTION = "abort";
    static final String NO_MESSAGE = "no_message";
    //prefix:
    static final String PREFIX_NEXT_STEP = "NextStep:";
    static final String PREFIX_CURRENT_STEP = "CurrentStep:";
    static final String PREFIX_CLIENT_ID = "ClientID:";
    static final String PREFIX_SET_CLIENT_ID = "SetClientID:";
    static final String PREFIX_RECEIVED_CLIENT_ID = "ReceivedClientID:";
    static final String PREFIX_CONNECTION = "Connection:";
    static final String PREFIX_SIMULATION = "Simulation:";
    static final String PREFIX_TOTAL_STEPS = "TotalSteps:";
    static final String PREFIX_DEPOSIT_TO = "DepositTo:";
    static final String PREFIX_BANK_NAME = "BankName:";
    static final String PREFIX_CLIENT_TO = "ClientTo:";
    static final String PREFIX_PERSON_NAME = "PersonName:";
    static final String PREFIX_TRANSFER_FROM = "TransferFrom:";
    static final String PREFIX_COUNTRY = "Country:";
    static final String PREFIX_PERSON_ID = "PersonId:";
    static final String PREFIX_AMOUNT = "Amount:";
    static final String PREFIX_ADD_PERSON = "AddPerson:";
    static final String PREFIX_PERSON_OBJECT = "PersonObject:";
    static final String PREFIX_BANK_OBJECT = "BankObject:";
    static final String PREFIX_BANK_INFORMATION = "BankInformation:";
    // Command related
    static final String COMMAND_ALL_CLIENTS_CONNECTED = "all_clients_connected";
    static final String COMMAND_SIMULATION_COMPLETED = "simulation_completed";
    static final String COMMAND_SPLITTER = ";";
    static final String COLON = ":";

    // Standard text for logs
    static final String SOCKET_IS_CLOSED = "Socket %s is closed";
}
