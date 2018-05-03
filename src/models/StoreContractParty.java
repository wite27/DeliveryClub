package models;

public class StoreContractParty extends ContractParty{

    public StoreContractParty() {
        this.id = "store";
    }

    @Override
    public boolean isStore() {
        return true;
    }
}
