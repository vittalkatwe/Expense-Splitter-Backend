package com.example.expensesplitbackend.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private double receivableAmount;
    private double amountToPay;
    private String username;
    private String email;


    @Enumerated(EnumType.STRING)
    private Currency preferredCurrency;

    public User() {}

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.preferredCurrency = Currency.INR;
    }

    public User(String username, String email, Currency preferredCurrency) {
        this.username = username;
        this.email = email;
        this.preferredCurrency = preferredCurrency;
    }


    public Currency getPreferredCurrency() {
        return preferredCurrency;
    }

    public void setPreferredCurrency(Currency preferredCurrency) {
        this.preferredCurrency = preferredCurrency;
    }

    public UUID getId() { return id; }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getReceivableAmount() {
        return receivableAmount;
    }

    public void setReceivableAmount(double receivableAmount) {
        this.receivableAmount = receivableAmount;
    }

    public double getAmountToPay() {
        return amountToPay;
    }

    public void setAmountToPay(double amountToPay) {
        this.amountToPay = amountToPay;
    }
}
