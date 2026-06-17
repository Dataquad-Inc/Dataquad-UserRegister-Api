package com.dataquadinc.repository;

import com.dataquadinc.model.WeekOffConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WeekOffConfigRepository extends JpaRepository<WeekOffConfig, Long> {
    List<WeekOffConfig> findByEntity(String entity);
}
