package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    boolean existsByEmployeeId(Long employeeId);

    Optional<User> findByEmployeeId(Long employeeId);
}
