package com.example.expensesplitbackend.repository;

import com.example.expensesplitbackend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    Group findGroupById(UUID groupId);
}
