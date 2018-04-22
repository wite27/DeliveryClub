package models;

import jade.core.AID;

import java.util.Objects;

public class DeliveryContract {
    public DeliveryContract(AID deliveryman, AID receiver, double cost, String point) {
        this.deliveryman = deliveryman;
        this.receiver = receiver;
        this.cost = cost;
        this.point = point;
    }

    private AID deliveryman;
    private AID receiver;
    private double cost;
    private String point;

    public AID getDeliveryman() {
        return deliveryman;
    }

    public AID getReceiver() {
        return receiver;
    }

    public double getCost() {
        return cost;
    }

    public String getPoint() {
        return point;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryContract that = (DeliveryContract) o;
        return  Objects.equals(deliveryman, that.deliveryman) &&
                Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryman, receiver);
    }
}
