package messages;

import java.util.UUID;

public class CheckChainRequestMessageContent {
    public String getCheckId() {
        return checkId;
    }

    public String getProducerId() {
        return producerId;
    }

    String checkId;
    String producerId;

    public CheckChainRequestMessageContent(String checkId, String producerId) {
        this.checkId = checkId;
        this.producerId = producerId;
    }

    public CheckChainRequestMessageContent(String producerId) {
        this.checkId = UUID.randomUUID().toString();
        this.producerId = producerId;
    }
}
