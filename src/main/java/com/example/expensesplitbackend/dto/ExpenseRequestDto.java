package com.example.expensesplitbackend.dto;


import com.example.expensesplitbackend.model.Currency;
import com.example.expensesplitbackend.model.Expense;
import jakarta.annotation.Nonnull;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.UUID;

public class ExpenseRequestDto {

    private String title;

    private Double amount;

    private Expense.SplitType splitType;

    private UUID paidBy;

    private Map<UUID, Double> splitData;
    private Currency currency;

    public ExpenseRequestDto() {}

    public ExpenseRequestDto(String title,  Double amount, Expense.SplitType splitType, UUID paidBy, Map<UUID, Double> splitData) {
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.paidBy = paidBy;
        this.splitData = splitData;
    }

    public ExpenseRequestDto(String title,  Double amount, Expense.SplitType splitType, UUID paidBy, Map<UUID, Double> splitData, Currency currency) {
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.paidBy = paidBy;
        this.splitData = splitData;
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Expense.SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(Expense.SplitType splitType) {
        this.splitType = splitType;
    }

    public UUID getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(UUID paidBy) {
        this.paidBy = paidBy;
    }

    public Map<UUID, Double> getSplitData() {
        return splitData;
    }

    public void setSplitData(Map<UUID, Double> splitData) {
        this.splitData = splitData;
    }
}

