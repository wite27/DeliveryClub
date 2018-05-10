package models.interfaces;

import helpers.AgentHelper;

public interface IShortContactInfo {
    String getProducerId();
    String getConsumerId();
    String getPoint();
    double getCost();

    public static String print(IShortContactInfo info) {
            return AgentHelper.getLocalName(info.getProducerId()) + " -> " +
                    AgentHelper.getLocalName(info.getConsumerId()) + " in " +
                    info.getPoint() + " for " + info.getCost();
    }
}
