package com.example.expensesplitbackend.model;


import jakarta.persistence.*;
import java.util.*;

@Entity
public class Settlement {
    @Id
    private UUID id;
    private UUID fromUser;
    private UUID toUser;
    private double amount;

    public Settlement() {}

    public Settlement(UUID fromUser, UUID toUser, double amount) {
        this.id = UUID.randomUUID();
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
    }

    public UUID getFromUser() { return fromUser; }
    public UUID getToUser() { return toUser; }
    public double getAmount() { return amount; }
}
