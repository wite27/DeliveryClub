package models;

import jade.core.AID;

import java.util.Objects;

public class ContractParty {
    private String id;
    private boolean isStore;

    public String getId() {
        return id;
    }
    public boolean isStore() {return isStore;}

    private ContractParty() {}

    public ContractParty(String id, boolean isStore)
    {
        this.id = id;
        this.isStore = isStore;
    }

    public static ContractParty agent(AID aid)
    {
        return new ContractParty(aid.getName(), false);
    }

    public static ContractParty store()
    {
        return new ContractParty("store", true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractParty that = (ContractParty) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

