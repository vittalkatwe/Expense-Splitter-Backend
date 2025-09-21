package com.example.expensesplitbackend.dto;

import com.example.expensesplitbackend.model.Currency;

import java.util.UUID;

public class UserBalanceDto {
    private UUID userId;
    private String username;
    private double amountToReceive;
    private double amountToPay;
    private Currency currency;

    public UserBalanceDto(UUID userId, String username, double amountToReceive, double amountToPay, Currency currency) {
        this.userId = userId;
        this.username = username;
        this.amountToReceive = amountToReceive;
        this.amountToPay = amountToPay;
        this.currency = currency;
    }

    public UserBalanceDto() {}


    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getAmountToReceive() { return amountToReceive; }
    public void setAmountToReceive(double amountToReceive) { this.amountToReceive = amountToReceive; }

    public double getAmountToPay() { return amountToPay; }
    public void setAmountToPay(double amountToPay) { this.amountToPay = amountToPay; }
}

