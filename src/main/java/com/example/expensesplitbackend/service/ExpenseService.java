package com.example.expensesplitbackend.service;


import com.example.expensesplitbackend.dto.ExpenseRequestDto;
import com.example.expensesplitbackend.dto.UserBalanceDto;
import com.example.expensesplitbackend.model.*;
import com.example.expensesplitbackend.model.Currency;
import com.example.expensesplitbackend.repository.ExpenseRepository;
import com.example.expensesplitbackend.repository.GroupRepository;
import com.example.expensesplitbackend.repository.SettlementRepository;
import com.example.expensesplitbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class ExpenseService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    private static final Map<Currency, Double> usdRates = Map.of(
            Currency.USD, 1.0,
            Currency.INR, 83.0,
            Currency.EUR, 1.1,
            Currency.GBP, 0.81,
            Currency.AUD, 1.50
    );

    public ExpenseService(UserRepository userRepository,
                          GroupRepository groupRepository,
                          ExpenseRepository expenseRepository,
                          SettlementRepository settlementRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    public User createUser(String name, String email, Currency preferredCurrency) throws Exception {
        if(userRepository.existsUsersByEmail(email)) throw new Exception("User already exists");
        User user = new User(name, email, preferredCurrency);
        return userRepository.save(user);
    }

    public Group createGroup(String name) {
        Group group = new Group(name);
        return groupRepository.save(group);
    }

    public void addMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.addMember(user);
        groupRepository.save(group);
    }




    public Expense addExpense(UUID groupId, ExpenseRequestDto dto) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Map<UUID, User> groupMembers = group.getMembers().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        User payer = groupMembers.get(dto.getPaidBy());
        if (payer == null) {
            throw new RuntimeException("Payer is not a member of this group");
        }

        Map<UUID, Double> debtsInUSD = new HashMap<>();
        double totalExpenseInUSD = convert(dto.getAmount(), payer.getPreferredCurrency(), Currency.USD);


        if (dto.getSplitType() == Expense.SplitType.EQUAL) {
            int n = groupMembers.size();
            if (n <= 0) throw new RuntimeException("Group has no members");
            double shareUSD = totalExpenseInUSD / n;

            for (User member : groupMembers.values()) {
                debtsInUSD.put(member.getId(), shareUSD);
            }
        } else if (dto.getSplitType() == Expense.SplitType.EXACT) {
            if (dto.getSplitData() == null || dto.getSplitData().isEmpty()) {
                throw new RuntimeException("Exact split requires splitData");
            }
            double sumSplitAmount = dto.getSplitData().values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sumSplitAmount - dto.getAmount()) > 0.01) {
                throw new RuntimeException("Split amounts do not sum to total expense");
            }

            for (Map.Entry<UUID, Double> entry : dto.getSplitData().entrySet()) {
                double shareUSD = convert(entry.getValue(), payer.getPreferredCurrency(), Currency.USD);
                debtsInUSD.put(entry.getKey(), shareUSD);
            }
        } else if (dto.getSplitType() == Expense.SplitType.PERCENTAGE) {
            if (dto.getSplitData() == null || dto.getSplitData().isEmpty()) {
                throw new RuntimeException("Percentage split requires splitData");
            }
            double totalPercent = dto.getSplitData().values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(totalPercent - 100.0) > 0.0001) {
                throw new RuntimeException("Percentages must sum to 100");
            }

            for (Map.Entry<UUID, Double> entry : dto.getSplitData().entrySet()) {
                double shareUSD = totalExpenseInUSD * (entry.getValue() / 100.0);
                debtsInUSD.put(entry.getKey(), shareUSD);
            }
        }

        double totalOwedToPayerInUSD = 0.0;

        for (Map.Entry<UUID, Double> debtEntry : debtsInUSD.entrySet()) {
            UUID userId = debtEntry.getKey();
            double userDebtInUSD = debtEntry.getValue();

            if (userId.equals(payer.getId())) {
                continue;
            }

            User debtor = groupMembers.get(userId);
            if (debtor == null) {
                throw new RuntimeException("Debtor with ID " + userId + " not found in group.");
            }

            double debtInUserCurrency = convert(userDebtInUSD, Currency.USD, debtor.getPreferredCurrency());

            debtor.setAmountToPay(debtor.getAmountToPay() + debtInUserCurrency);

            totalOwedToPayerInUSD += userDebtInUSD;
        }

        double totalOwedInPayerCurrency = convert(totalOwedToPayerInUSD, Currency.USD, payer.getPreferredCurrency());

        payer.setReceivableAmount(payer.getReceivableAmount() + totalOwedInPayerCurrency);

        for (User user : groupMembers.values()) {
            user.setAmountToPay(round(user.getAmountToPay()));
            user.setReceivableAmount(round(user.getReceivableAmount()));
        }
        userRepository.saveAll(groupMembers.values());
        Map<UUID, Double> splitDataForExpense = new HashMap<>();
        for (Map.Entry<UUID, Double> debtEntry : debtsInUSD.entrySet()) {
            if (!debtEntry.getKey().equals(payer.getId())) {
                splitDataForExpense.put(debtEntry.getKey(), debtEntry.getValue());
            }
        }

        Expense expense = new Expense(dto.getTitle(), totalExpenseInUSD, dto.getSplitType(), dto.getPaidBy(), splitDataForExpense);
        return expenseRepository.save(expense);
    }


    public void settleDebt(UUID groupId, UUID fromUserId, UUID toUserId, double amount) {
        User from = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("Settling user not found"));
        User to = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Receiving user not found"));

        if(amount>from.getAmountToPay()) throw new RuntimeException("Cannot pay more than amount to pay");
        if(amount>to.getReceivableAmount()) throw new RuntimeException("Cannot pay more than amount to receive");
        double settlementAmountInFromCurrency = amount;

        Map<UUID, Double> balancesInFromCurrency = calculateBalances(groupId, from.getPreferredCurrency());
        double fromUserBalance = balancesInFromCurrency.getOrDefault(fromUserId, 0.0);

        if (fromUserBalance >= -1e-6) {
            throw new RuntimeException("Invalid settlement: Payer has no outstanding debt.");
        }

        double fromUserTotalDebt = -fromUserBalance;
        if (settlementAmountInFromCurrency > fromUserTotalDebt + 1e-6) {
            throw new RuntimeException(
                    String.format("Invalid settlement amount. Amount %.2f %s exceeds total debt of %.2f %s.",
                            settlementAmountInFromCurrency, from.getPreferredCurrency(),
                            fromUserTotalDebt, from.getPreferredCurrency())
            );
        }

        Map<UUID, Double> balancesInToCurrency = calculateBalances(groupId, to.getPreferredCurrency());
        double toUserBalance = balancesInToCurrency.getOrDefault(toUserId, 0.0);
        double toUserReceivable = Math.max(toUserBalance, 0.0);

        double settlementAmountInToCurrency = convert(settlementAmountInFromCurrency, from.getPreferredCurrency(), to.getPreferredCurrency());
        if (settlementAmountInToCurrency - toUserReceivable > 1e-6) {
            throw new RuntimeException(
                    String.format("Invalid settlement amount. Amount %.2f %s exceeds receiver's receivable %.2f %s.",
                            settlementAmountInToCurrency, to.getPreferredCurrency(),
                            toUserReceivable, to.getPreferredCurrency())
            );
        }

        double settlementAmountUSD = convert(settlementAmountInFromCurrency, from.getPreferredCurrency(), Currency.USD);
        Settlement settlement = new Settlement(fromUserId, toUserId, settlementAmountUSD);
        settlementRepository.save(settlement);

        from.setAmountToPay(from.getAmountToPay() - settlementAmountInFromCurrency);
        to.setReceivableAmount(to.getReceivableAmount() - settlementAmountInToCurrency);

        from.setAmountToPay(round(from.getAmountToPay()));
        to.setReceivableAmount(round(to.getReceivableAmount()));

        userRepository.save(from);
        userRepository.save(to);
    }


    public List<UserBalanceDto> getUserBalances(UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        Map<UUID, Double> balancesUSD = calculateBalancesInUSD(groupId);
        List<UserBalanceDto> result = new ArrayList<>();
        for (User u : group.getMembers()) {
            double balance = balancesUSD.getOrDefault(u.getId(), 0.0);
            double toReceive = balance > 0 ? round(convert(balance, Currency.USD, u.getPreferredCurrency())) : 0.0;
            double toPay = balance < 0 ? round(convert(-balance, Currency.USD, u.getPreferredCurrency())) : 0.0;
            result.add(new UserBalanceDto(u.getId(), u.getUsername(), toReceive, toPay, u.getPreferredCurrency()));
        }
        return result;
    }




    public double convert(double amount, Currency from, Currency to) {
        if (from == to) return amount;
        double amountInUSD = (from == Currency.USD) ? amount : amount / usdRates.get(from);
        return (to == Currency.USD) ? amountInUSD : amountInUSD * usdRates.get(to);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }


    public Map<UUID, Double> calculateBalances(UUID groupId, Currency targetCurrency) {
        Map<UUID, Double> balancesUSD = calculateBalancesInUSD(groupId);
        Map<UUID, Double> targetBalances = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : balancesUSD.entrySet()) {
            double convertedBalance = convert(entry.getValue(), Currency.USD, targetCurrency);
            targetBalances.put(entry.getKey(), convertedBalance);
        }
        return targetBalances;
    }

    private Map<UUID, Double> calculateBalancesInUSD(UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        Map<UUID, Double> balances = new HashMap<>();
        for (User u : group.getMembers()) balances.put(u.getId(), 0.0);

        List<Expense> expenses = expenseRepository.findAll();
        for (Expense e : expenses) {
            double sumOwedToPayer = 0.0;
            for (Map.Entry<UUID, Double> entry : e.getSplitData().entrySet()) {
                UUID debtor = entry.getKey();
                double share = entry.getValue();
                if (balances.containsKey(debtor)) {
                    balances.put(debtor, balances.get(debtor) - share);
                    sumOwedToPayer += share;
                }
            }
            UUID payer = e.getPaidBy();
            if (balances.containsKey(payer)) {
                balances.put(payer, balances.get(payer) + sumOwedToPayer);
            }
        }
        List<Settlement> settlements = settlementRepository.findAll();
        for (Settlement s : settlements) {
            if (balances.containsKey(s.getFromUser())) {
                balances.put(s.getFromUser(), balances.get(s.getFromUser()) + s.getAmount());
            }
            if (balances.containsKey(s.getToUser())) {
                balances.put(s.getToUser(), balances.get(s.getToUser()) - s.getAmount());
            }
        }
        return balances;
    }





    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group findGroupById(UUID groupId) {
        return groupRepository.findGroupById(groupId);
    }

    public boolean validateAmountCheck(double amount, Map<UUID, Double> splitData) {

        if(splitData==null) return true;
        double total=0;
        Map<UUID, Double> splitDataMap = splitData;

        for (Map.Entry<UUID, Double> entry : splitDataMap.entrySet()) {
            total+=entry.getValue();
        }
        return total==amount;

    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}