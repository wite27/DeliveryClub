package models;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeliveryContract {
    public DeliveryContract(
            ContractParty producer,
            ContractParty consumer,
            double cost,
            String point,
            ArrayList<DeliveryContract> previousContracts) {
        this.producer = producer;
        this.consumer = consumer;
        this.cost = cost;
        this.point = point;
        this.previousContracts = previousContracts;
    }

    private DeliveryContract() {}

    private String id = UUID.randomUUID().toString();
    private ContractParty producer;
    private ContractParty consumer;
    private double cost;
    private String point;
    private ArrayList<DeliveryContract> previousContracts;

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

    public ArrayList<DeliveryContract> getPreviousContracts() { return previousContracts; }

    public ArrayList<DeliveryContract> makeChain() {
        var chain = new ArrayList<>(getPreviousContracts());
        chain.add(0, this);

        return chain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryContract that = (DeliveryContract) o;
        return this.id.equals(that.id);

        //if (previousContracts.size() != that.previousContracts.size())
        //    return false; //TODO compare each contract
        //return  Objects.equals(producer, that.producer) &&
        //        Objects.equals(consumer, that.consumer);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
