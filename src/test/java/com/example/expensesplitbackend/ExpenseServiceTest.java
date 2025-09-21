package com.example.expensesplitbackend;

import com.example.expensesplitbackend.dto.ExpenseRequestDto;
import com.example.expensesplitbackend.dto.UserBalanceDto;
import com.example.expensesplitbackend.model.Currency;
import com.example.expensesplitbackend.model.Expense;
import com.example.expensesplitbackend.model.Group;
import com.example.expensesplitbackend.model.User;
import com.example.expensesplitbackend.service.ExpenseService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class ExpenseServiceTest {

    @Autowired
    private ExpenseService service;

    private User user1, user2, user3;
    private Group group;

    @BeforeEach
    void setup() throws Exception {
        group = service.createGroup("Trip");

        user1 = service.createUser("User1", "u1@gmail.com", Currency.USD);
        user2 = service.createUser("User2", "u2@gmail.com", Currency.USD);
        user3 = service.createUser("User3", "u3@gmail.com", Currency.INR);

        service.addMember(group.getId(), user1.getId());
        service.addMember(group.getId(), user2.getId());
        service.addMember(group.getId(), user3.getId());
    }

    private double expectedFor(User user, double amount, Currency expenseCurrency) {
        return service.convert(amount, expenseCurrency, user.getPreferredCurrency());
    }

    @Test
    void testEqualSplit_MixedCurrencies() {
        Expense expense = service.addExpense(group.getId(),
                new ExpenseRequestDto("Dinner", 90.0, Expense.SplitType.EQUAL, user1.getId(), null));

        List<UserBalanceDto> balances = service.getUserBalances(group.getId());

        for (UserBalanceDto b : balances) {
            if (b.getUserId().equals(user1.getId())) {
                assertEquals(expectedFor(user1, 60.0, user1.getPreferredCurrency()), b.getAmountToReceive(), 0.01);
                assertEquals(0.0, b.getAmountToPay(), 0.01);
            } else if (b.getUserId().equals(user2.getId()) || b.getUserId().equals(user3.getId())) {
                assertEquals(0.0, b.getAmountToReceive(), 0.01);
                assertEquals(expectedFor(b.getUserId().equals(user2.getId()) ? user2 : user3, 30.0, user1.getPreferredCurrency()),
                        b.getAmountToPay(), 0.01);
            }
        }
    }

    @Test
    void testExactAmountSplit_MixedCurrencies() {
        Map<UUID, Double> split = new HashMap<>();
        split.put(user1.getId(), 70.0);
        split.put(user2.getId(), 30.0);

        Expense expense = service.addExpense(group.getId(),
                new ExpenseRequestDto("Lunch", 100.0, Expense.SplitType.EXACT, user3.getId(), split));

        List<UserBalanceDto> balances = service.getUserBalances(group.getId());

        for (UserBalanceDto b : balances) {
            if (b.getUserId().equals(user1.getId()))
                assertEquals(expectedFor(user1, 70.0, user3.getPreferredCurrency()), b.getAmountToPay(), 0.01);
            if (b.getUserId().equals(user2.getId()))
                assertEquals(expectedFor(user2, 30.0, user3.getPreferredCurrency()), b.getAmountToPay(), 0.01);
            if (b.getUserId().equals(user3.getId()))
                assertEquals(expectedFor(user3, 100.0, user3.getPreferredCurrency()), b.getAmountToReceive(), 0.01);
        }
    }

    @Test
    void testPercentageSplit_MixedCurrencies() {
        Map<UUID, Double> split = new HashMap<>();
        split.put(user1.getId(), 60.0); // 60%
        split.put(user2.getId(), 40.0); // 40%

        Expense expense = service.addExpense(group.getId(),
                new ExpenseRequestDto("Shopping", 200.0, Expense.SplitType.PERCENTAGE, user3.getId(), split));

        List<UserBalanceDto> balances = service.getUserBalances(group.getId());

        for (UserBalanceDto b : balances) {
            if (b.getUserId().equals(user1.getId()))
                assertEquals(expectedFor(user1, 120.0, user3.getPreferredCurrency()), b.getAmountToPay(), 0.01);
            if (b.getUserId().equals(user2.getId()))
                assertEquals(expectedFor(user2, 80.0, user3.getPreferredCurrency()), b.getAmountToPay(), 0.01);
            if (b.getUserId().equals(user3.getId()))
                assertEquals(expectedFor(user3, 200.0, user3.getPreferredCurrency()), b.getAmountToReceive(), 0.01);
        }
    }

    @Test
    void testSettlingDebt_MixedCurrencies() {
        Expense expense = service.addExpense(group.getId(),
                new ExpenseRequestDto("Snack", 90.0, Expense.SplitType.EQUAL, user1.getId(), null));

        // User2 settles 30 USD (in their currency)
        service.settleDebt(group.getId(), user2.getId(), user1.getId(), 30.0);

        List<UserBalanceDto> balancesAfter = service.getUserBalances(group.getId());

        for (UserBalanceDto b : balancesAfter) {
            if (b.getUserId().equals(user2.getId()))
                assertEquals(0.0, b.getAmountToPay(), 0.01);
            if (b.getUserId().equals(user1.getId()))
                assertEquals(expectedFor(user1, 30.0, user1.getPreferredCurrency()), b.getAmountToReceive(), 0.01);
        }
    }
}