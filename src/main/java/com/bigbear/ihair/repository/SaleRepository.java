package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Sale;
import com.bigbear.ihair.entity.enums.SaleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Override
    @EntityGraph(attributePaths = {"salon", "customer", "sourceAppointment", "createdBy", "campaign"})
    Page<Sale> findAll(Specification<Sale> specification, Pageable pageable);

    List<Sale> findAllByOrderByCreatedAtDesc();

    List<Sale> findAllBySalonIdInOrderByCreatedAtDesc(Iterable<Long> salonIds);

    List<Sale> findAllByStatusOrderByCreatedAtDesc(SaleStatus status);

    List<Sale> findAllBySalonIdInAndStatusOrderByCreatedAtDesc(
            Iterable<Long> salonIds, SaleStatus status);

    boolean existsBySourceAppointmentId(Long appointmentId);

    Optional<Sale> findBySourceAppointmentId(Long appointmentId);

    boolean existsBySourceAppointmentIdAndIdNot(Long appointmentId, Long saleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findWithLockById(@Param("id") Long id);

    @Query("""
            select distinct s from Sale s
            left join fetch s.items
            where s.status = :status
              and s.completedAt >= :from
              and s.completedAt < :to
            """)
    List<Sale> findAllForRevenue(
            @Param("status") SaleStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select distinct s from Sale s
            left join fetch s.items
            where s.status = :status
              and s.completedAt >= :from
              and s.completedAt < :to
              and s.salon.id in :salonIds
            """)
    List<Sale> findForRevenueBySalonIds(
            @Param("status") SaleStatus status,
            @Param("salonIds") Iterable<Long> salonIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
