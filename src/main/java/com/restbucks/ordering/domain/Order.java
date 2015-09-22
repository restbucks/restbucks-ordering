package com.restbucks.ordering.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_order")
@Getter
@NoArgsConstructor
public class Order {
    @Id
    @Column(name = "id")
    private String trackingId;
    private String location;
    private String status;
    private double cost;

    @ElementCollection
    @CollectionTable(name = "t_order_item",
            joinColumns = {@JoinColumn(name = "order_id")})
    private List<Item> items = new ArrayList<>();

    public Order(String trackingId) {
        this.trackingId = trackingId;
        this.status = "pending";
    }

    public void locationIs(String location) {
        this.location = location;
    }

    public void append(List<Item> items) {
        this.items.addAll(items);
        this.cost = this.items.stream()
                .map(i -> i.subtotal())
                .reduce(0.0, (accumulator, _item) -> accumulator + _item);
    }


    @NoArgsConstructor
    @EqualsAndHashCode
    @ToString
    @Getter
    @Embeddable
    public static class Item {
        private String name;
        private int quantity;
        private String milk;
        private String size;
        private double price;

        public Item(String name, int quantity, String milk, String size, double price) {
            this.name = name;
            this.quantity = quantity;
            this.milk = milk;
            this.size = size;
            this.price = price;
        }

        public double subtotal() {
            return getQuantity() * getPrice();
        }
    }
}