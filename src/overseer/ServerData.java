package overseer;

public class ServerData {
    private Server server;
    private String stepNumber;
    private Integer connectionLimit;
    private Integer currentConnections;

    ServerData(Server server, String stepNumber, Integer connectionLimit, Integer currentConnections) {
        this.server = server;
        this.stepNumber = stepNumber;
        this.connectionLimit = connectionLimit;
        this.currentConnections = currentConnections;
    }

    ServerData() {
        this.server = new Server();
        this.stepNumber = "";
        this.connectionLimit = 0;
        this.currentConnections = 0;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(String stepNumber) {
        this.stepNumber = stepNumber;
    }

    public Integer getConnectionLimit() {
        return connectionLimit;
    }

    public void setConnectionLimit(int connectionLimit) {
        this.connectionLimit = connectionLimit;
    }

    public Integer getCurrentConnections() {
        return currentConnections;
    }

    public void setCurrentConnections(Integer currentConnections) {
        System.out.println("New current connections: " + currentConnections);
        this.currentConnections = currentConnections;
    }
}
