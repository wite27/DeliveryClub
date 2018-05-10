package helpers;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import static com.alibaba.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect;

public class MessageHelper {
    public static <T> ACLMessage buildMessage(int performative, Class<T> type, T content)
    {
        var message = new ACLMessage(performative);

        message.setOntology(type.getName());
        message.setContent(JSON.toJSONString(content, DisableCircularReferenceDetect));

        return message;
    }

    public static ACLMessage addReceivers(ACLMessage message, Iterable<AID> receivers)
    {
        receivers.forEach(message::addReceiver);
        return message;
    }

    public static <T> T parse(ACLMessage message, Class<T> type)
    {
        return new Gson().fromJson(message.getContent(), type);
    }
}

