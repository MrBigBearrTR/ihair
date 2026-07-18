package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByActiveTrue();

    List<Customer> findAllBySalonIdAndActiveTrue(Long salonId);

    List<Customer> findAllBySalonIdInAndActiveTrue(Iterable<Long> salonIds);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
