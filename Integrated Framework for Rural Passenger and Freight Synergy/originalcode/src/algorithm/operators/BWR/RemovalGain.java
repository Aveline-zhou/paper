package algorithm.operators.BWR;

import model.Customer;


public class RemovalGain {
    private Customer customer;
    private double gain;

    public RemovalGain() {
    }

    public RemovalGain(Customer customer, double gain) {
        this.customer = customer;
        this.gain = gain;
    }

    // Getter and Setter methods
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    @Override
    public String toString() {
        return "RemovalGain{customer=" + customer.getId() + ", gain=" + gain + "}";
    }
}