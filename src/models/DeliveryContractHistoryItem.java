package models;

public class DeliveryContractHistoryItem {
    private String id;
    private ContractParty producer;
    private ContractParty consumer;
    private double cost;
    private String point;

    public DeliveryContractHistoryItem(String id,
                                       ContractParty producer, ContractParty consumer,
                                       double cost, String point) {
        this.id = id;
        this.producer = producer;
        this.consumer = consumer;
        this.cost = cost;
        this.point = point;
    }

    public static DeliveryContractHistoryItem fromContract(DeliveryContract contract) {
        return new DeliveryContractHistoryItem(contract.getId(), contract.getProducer(),
                contract.getConsumer(), contract.getCost(), contract.getPoint());
    }

    public String getId() {return id;}

    public ContractParty getProducer() {
        return producer;
    }

    public ContractParty getConsumer() {
        return consumer;
    }

    public double getCost() {
        return cost;
    }

    public String getPoint() {
        return point;
    }
}
