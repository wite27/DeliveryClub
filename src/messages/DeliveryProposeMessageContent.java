package messages;

import java.util.ArrayList;
import java.util.UUID;

public class DeliveryProposeMessageContent {
    public String proposeId;
    public ArrayList<String> points;
    public double cost;

    private DeliveryProposeMessageContent() {}

    public DeliveryProposeMessageContent(ArrayList<String> points, double cost) {
        this.proposeId = UUID.randomUUID().toString();
        this.points = points;
        this.cost = cost;
    }
}
