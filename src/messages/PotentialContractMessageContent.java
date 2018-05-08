package messages;

public class PotentialContractMessageContent {
    private String proposeId;
    public String point;
    public double cost;

    private PotentialContractMessageContent() {}

    public PotentialContractMessageContent(String proposeId, String point, double cost) {
        this.proposeId = proposeId;
        this.point = point;
        this.cost = cost;
    }

    public String getProposeId() {
        return proposeId;
    }
}
