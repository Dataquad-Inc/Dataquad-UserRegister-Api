package com.dataquadinc.repository;

import com.dataquadinc.model.UserProfileDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProfileDocumentRepository extends JpaRepository<UserProfileDocument, Long> {
    List<UserProfileDocument> findByUserId(String userId);


}
