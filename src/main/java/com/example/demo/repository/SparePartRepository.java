package com.example.demo.repository;

import com.example.demo.model.SparePart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    /**
     * Пессимистическая блокировка строки: конкурентные резервы/списания склада
     * выполняются последовательно, чтобы не нарушать инвариант reserved <= stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SparePart p where p.id = :id")
    Optional<SparePart> findByIdForUpdate(@Param("id") Long id);
}
