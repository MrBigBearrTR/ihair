package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByActiveTrue();

    List<Employee> findAllBySalonIdAndActiveTrue(Long salonId);

    List<Employee> findAllBySalonIdInAndActiveTrue(Iterable<Long> salonIds);

    Optional<Employee> findByIdAndActiveTrue(Long id);

    Optional<Employee> findByIdAndSalonIdInAndActiveTrue(Long id, Iterable<Long> salonIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findWithLockById(@Param("id") Long id);
}
