package helpers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.gson.Gson;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import messages.DeliveryProposeMessageContent;

import java.util.ArrayList;

import static com.alibaba.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect;

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

    public static <T> ACLMessage buildMessage2(int performative, Class<T> type, T content)
    {
        var message = new ACLMessage(performative);

        message.setOntology(type.getName());
        message.setContent(JSON.toJSONString(content, DisableCircularReferenceDetect));

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

    public static <T> T parse(ACLMessage message, Class<T> type)
    {
        return new Gson().fromJson(message.getContent(), type);
    }
}

