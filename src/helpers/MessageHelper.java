package helpers;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import models.Consts;

public class MessageHelper {
    private final static String delimiter = "/";

    public static ACLMessage BuildMessage(int performative, String prefix, String... params)
    {
        var message = new ACLMessage(performative);

        message.setContent(prefix + delimiter + String.join(delimiter, params));
        return message;
    }
}

