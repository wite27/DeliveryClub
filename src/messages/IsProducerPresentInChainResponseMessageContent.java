package messages;

public class IsProducerPresentInChainResponseMessageContent {
    private boolean isPresent;

    public boolean isPresent() {
        return isPresent;
    }

    public IsProducerPresentInChainResponseMessageContent(boolean isPresent) {
        this.isPresent = isPresent;
    }
}
