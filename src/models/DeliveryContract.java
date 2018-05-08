package models;

import com.alibaba.fastjson.annotation.JSONField;
import jade.core.AID;

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
            ArrayList<DeliveryContractHistoryItem> previousContracts) {
        this.producer = producer;
        this.consumer = consumer;
        this.cost = cost;
        this.point = point;
        this.previousContracts = previousContracts;

        this.id = UUID.randomUUID().toString();
    }

    private DeliveryContract() {}

    @JSONField(ordinal = 1)
    private String id;

    @JSONField(ordinal = 2)
    private ContractParty producer;

    @JSONField(ordinal = 3)
    private ContractParty consumer;

    @JSONField(ordinal = 4)
    private double cost;

    @JSONField(ordinal = 5)
    private String point;

    @JSONField(ordinal = 6)
    private ArrayList<DeliveryContractHistoryItem> previousContracts;

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

    public ArrayList<DeliveryContractHistoryItem> getPreviousContracts() { return previousContracts; }

    public ArrayList<DeliveryContractHistoryItem> makeChain() {
        var chain = new ArrayList<>(getPreviousContracts());
        chain.add(0, DeliveryContractHistoryItem.fromContract(this));

        return chain;
    }

    public boolean isProducerInThisChain(AID agent) {
        return this.producer.getId().equals(agent.getName())
                || previousContracts.stream()
                .map(DeliveryContractHistoryItem::getProducer)
                .anyMatch(x -> x.getId().equals(agent.getName()));
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
