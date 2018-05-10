package factories;

import helpers.StringHelper;
import jade.lang.acl.MessageTemplate;

public class MessageTemplateFactory {
    public static MessageTemplate create(int performative, Class type) {
        return new MessageTemplate(x -> x.getPerformative() == performative
                                        && StringHelper.safeEquals(x.getOntology(), type.getName()));
    }

    public static MessageTemplate create(int performative, Class type, String conversationId) {
        return new MessageTemplate(x -> x.getPerformative() == performative
                && StringHelper.safeEquals(x.getOntology(), type.getName())
                && StringHelper.safeEquals(x.getConversationId(), conversationId));
    }

    public static MessageTemplate create(int performative1, int performative2, Class type) {
        return new MessageTemplate(x -> (x.getPerformative() == performative1
                                        || x.getPerformative() == performative2)
                                        && StringHelper.safeEquals(x.getOntology(), type.getName()));
    }
}
