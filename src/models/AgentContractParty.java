package models;

import jade.core.AID;

public class AgentContractParty extends ContractParty{

    public AgentContractParty(AID id) {
        id = id;
    }

    @Override
    public boolean isStore() {
        return false;
    }

}
