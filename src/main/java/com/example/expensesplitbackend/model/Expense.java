package com.example.expensesplitbackend.model;

import jakarta.persistence.*;

import java.util.*;

@Entity
public class Expense {
    public enum SplitType { EQUAL, EXACT, PERCENTAGE }

    @Id
    private UUID id=UUID.randomUUID();
    private String title;
    private double amount;

    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    private UUID paidBy;

    @ElementCollection
    private Map<UUID, Double> splitData = new HashMap<>();

    public Expense() {}

    public Expense(String title, double amount, SplitType splitType, UUID paidBy, Map<UUID, Double> splitData) {
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.paidBy = paidBy;
        this.splitData = splitData;
    }

    public Expense(String title, double amount, SplitType splitType, UUID paidBy) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.paidBy = paidBy;
        this.splitData = new HashMap<UUID, Double>();
    }

    public UUID getId() { return id; }
    public double getAmount() { return amount; }
    public SplitType getSplitType() { return splitType; }
    public Map<UUID, Double> getSplitData() { return splitData; }
    public UUID getPaidBy() { return paidBy; }
}
