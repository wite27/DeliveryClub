package models;

import com.alibaba.fastjson.annotation.JSONField;
import helpers.AgentHelper;
import jade.core.AID;

import java.util.ArrayList;
import java.util.UUID;

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
    public boolean isProducerDelivery;

    @JSONField(ordinal = 7)
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

    // TODO maybe use immutable contracts?
    public void updateCost(double cost) {
        this.cost = cost;
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

    public boolean hasEqualProducersChain(ArrayList<DeliveryContractHistoryItem> other) {
        if (this.previousContracts.size() != other.size())
            return false;

        for (int i = 0; i < this.previousContracts.size(); i++) {
            if (!this.previousContracts.get(i).getProducer()
                    .equals(other.get(i).getProducer()))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toShortString()
    {
        return AgentHelper.getLocalName(producer.getId()) + " -> " +
                AgentHelper.getLocalName(consumer.getId()) + " in " +
                point + " for " + cost;
    }
}
