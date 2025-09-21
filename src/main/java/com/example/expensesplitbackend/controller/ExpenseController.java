package com.example.expensesplitbackend.controller;


import com.example.expensesplitbackend.dto.ExpenseRequestDto;
import com.example.expensesplitbackend.dto.UserBalanceDto;
import com.example.expensesplitbackend.model.*;
import com.example.expensesplitbackend.model.Currency;
import com.example.expensesplitbackend.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ExpenseController {
    private final ExpenseService service;

    @Autowired
    public ExpenseController(ExpenseService service) { this.service = service; }


    @PostMapping("/users")
    public User createUser(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam Currency preferredCurrency) throws Exception{
        return service.createUser(name, email, preferredCurrency);
    }


    @GetMapping("/users")
    public List<User> getUsers(){
        return service.getUsers();
    }


    @PostMapping("/groups")
    public Group createGroup(@RequestParam String name) { return service.createGroup(name); }

    @GetMapping("/groups")
    public List<Group> getAllGroups(){
        return service.getAllGroups();
    }

    @GetMapping("/group/{groupId}")
    public Group findGroupById(@PathVariable UUID groupId) {
        return service.findGroupById(groupId);
    }

    @PostMapping("/groups/{groupId}/members")
    public String addMember(@PathVariable UUID groupId, @RequestParam UUID userId) {
        service.addMember(groupId, userId);
        return "Added member";
    }

    @GetMapping("/groups/{groupId}/user-balances")
    public List<UserBalanceDto> getUserBalances(@PathVariable UUID groupId) {
        return service.getUserBalances(groupId);
    }


    @PostMapping("/groups/{groupId}/expenses")
    public Expense addExpense(@PathVariable UUID groupId,
                              @RequestBody ExpenseRequestDto expenseRequestDto) throws Exception {
        if(expenseRequestDto.getAmount()==null) throw new Exception("Amount cannot be null");
        if(expenseRequestDto.getTitle()==null) throw new Exception("Title cannot be null");
        if(expenseRequestDto.getSplitData()==null && expenseRequestDto.getSplitType() != Expense.SplitType.EQUAL) throw new Exception("SplitData cannot be null");
        if(expenseRequestDto.getSplitType()==null) throw new Exception("SplitType cannot be null");
        if(!service.validateAmountCheck(expenseRequestDto.getAmount(), expenseRequestDto.getSplitData()) && expenseRequestDto.getSplitType()!=Expense.SplitType.PERCENTAGE) throw new Exception("No clarity if the entire amount is paid or maybe the amount is overpaid.");
        return service.addExpense(groupId, expenseRequestDto);
    }

    @PostMapping("/groups/{groupId}/settle")
    public void settleDebt(@PathVariable UUID groupId,
                           @RequestParam UUID fromUser,
                           @RequestParam UUID toUser,
                           @RequestParam double amount) {
        service.settleDebt(groupId, fromUser, toUser, amount);
    }


}

