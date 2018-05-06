package helpers;

import com.alibaba.fastjson.JSON;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import messages.DeliveryProposeMessageContent;

import java.util.ArrayList;

public class MessageHelper {
    private final static String delimiter = "/";

    public static ACLMessage buildMessage(int performative, String prefix, String... params)
    {
        var message = new ACLMessage(performative);

        var content = prefix;
        if (params != null && params.length > 0)
            content += delimiter + String.join(delimiter, params);

        message.setContent(content);
        return message;
    }

    public static ACLMessage buildMessage2(int performative, String messageType, Object content)
    {
        var message = new ACLMessage(performative);

        message.setOntology(messageType);
        message.setContent(JSON.toJSONString(content));

        return message;
    }

    public static String[] getParams(ACLMessage message)
    {
        return message.getContent().split(delimiter);
    }

    public static ACLMessage addReceivers(ACLMessage message, ArrayList<DFAgentDescription> receivers)
    {
        receivers.forEach(x -> message.addReceiver(x.getName()));
        return message;
    }

    public static DeliveryProposeMessageContent getDeliveryProposeMessageContent(String content)
    {
        return JSON.parseObject(content, DeliveryProposeMessageContent.class);
    }
}

