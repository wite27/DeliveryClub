package models;

import jade.core.AID;

import java.util.Objects;

public class ContractParty {
    private String id;
    private boolean buzulka;

    public String getId() {
        return id;
    }
    public boolean isBuzulka() {return buzulka;}

    private ContractParty() {}

    public ContractParty(String id, boolean buzulka)
    {
        this.id = id;
        this.buzulka = buzulka;
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

