package messages;

public class CallForDeliveryProposeMessageContent {
    private boolean needDeliveryToPoint;
    private String neededPoint;

    public CallForDeliveryProposeMessageContent() {}

    public CallForDeliveryProposeMessageContent(String neededPoint) {
        this.needDeliveryToPoint = true;
        this.neededPoint = neededPoint;
    }

    public boolean isNeedDeliveryToPoint() {
        return needDeliveryToPoint;
    }

    public String getNeededPoint() {
        return neededPoint;
    }
}
