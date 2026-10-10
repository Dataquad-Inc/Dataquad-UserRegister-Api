package com.dataquadinc.repository;

import com.dataquadinc.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginRepository extends JpaRepository<UserDetails, Long> {
    UserDetails findByEmail(String email);

    UserDetails findByEmailAndTenantId(String email, String tenantId);
}
