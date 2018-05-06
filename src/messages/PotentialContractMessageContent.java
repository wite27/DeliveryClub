package messages;

import com.alibaba.fastjson.JSON;
import jade.lang.acl.ACLMessage;

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

    public static PotentialContractMessageContent fromMessage(ACLMessage message)
    {
        return JSON.parseObject(message.getContent(), PotentialContractMessageContent.class);
    }

    public String getProposeId() {
        return proposeId;
    }
}
