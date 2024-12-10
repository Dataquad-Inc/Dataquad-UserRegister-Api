package com.dataquadinc.repository;

import com.dataquadinc.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<UserDetails,Integer> {
    UserDetails findByEmail(String email) ;

    UserDetails findByUserId( String userId);
    UserDetails findByUserName(String userName);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);
}
