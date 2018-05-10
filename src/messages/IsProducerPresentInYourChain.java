package messages;

import java.util.UUID;

public class IsProducerPresentInYourChain {
    public String getCheckId() {
        return checkId;
    }

    public String getProducerId() {
        return producerId;
    }

    String checkId;
    String producerId;

    public IsProducerPresentInYourChain(String checkId, String producerId) {
        this.checkId = checkId;
        this.producerId = producerId;
    }

    public IsProducerPresentInYourChain(String producerId) {
        this.checkId = UUID.randomUUID().toString();
        this.producerId = producerId;
    }
}
