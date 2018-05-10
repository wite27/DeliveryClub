package messages;

public class CheckChainResponseMessageContent {
    private boolean isPresent;

    public boolean isPresent() {
        return isPresent;
    }

    public CheckChainResponseMessageContent(boolean isPresent) {
        this.isPresent = isPresent;
    }
}
