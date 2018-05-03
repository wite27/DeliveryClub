package models;

import java.util.Objects;

public abstract class ContractParty {
    protected String id;
    public abstract boolean isStore();
    public String getId() {
        return id;
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

