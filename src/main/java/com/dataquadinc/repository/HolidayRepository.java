package com.dataquadinc.repository;

import com.dataquadinc.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByHolidayDate(LocalDate date);

    List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findHolidayDatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByHolidayDate(LocalDate date);
}
