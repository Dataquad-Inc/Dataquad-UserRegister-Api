package com.dataquadinc.repository;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserDao extends JpaRepository<UserDetails, Integer> {

    UserDetails findByEmail(String email);
    UserDetails findByUserId(String userId);
    @Query("SELECT u FROM UserDetails u JOIN u.roles r " +
            "WHERE (:userId IS NULL OR u.userId = :userId) " +
            "AND (:roleEnum IS NULL OR r.name = :roleEnum)")
    List<UserDetails> findByUserIdAndRole(@Param("userId") String userId,
                                          @Param("roleEnum") UserType roleEnum);

    // ✅ Fetch only BDM employees
    @Query("SELECT u FROM UserDetails u JOIN u.roles r WHERE r.name = 'BDM'")
    List<UserDetails> findBdmEmployees();

    // ✅ Count Clients based on User ID
    @Query(value = """
        SELECT COUNT(*) 
        FROM bdm_client 
        WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId)
    """, nativeQuery = true)
    long countClientsByUserId(@Param("userId") String userId);

    // ✅ Fetch client names for a specific BDM (based on userId)
    @Query(value = """
        SELECT client_name 
        FROM bdm_client 
        WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId)
    """, nativeQuery = true)
    List<String> findClientNamesByUserId(@Param("userId") String userId);

    // Count ALL submissions across all job IDs and clients (across all users)
    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions cs
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
""", nativeQuery = true)
    long countAllSubmissionsAcrossAllJobsAndClients();

    // Count ALL submissions for a specific client across all job IDs
    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions cs
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
""", nativeQuery = true)
    long countAllSubmissionsByClientName(@Param("clientName") String clientName);

    // Count ALL interviews for a client across ALL job IDs
    @Query(value = """
    SELECT COUNT(*) 
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE (b.client_name = :clientName OR r.client_name = :clientName 
           OR (:clientName IS NULL AND EXISTS (
                SELECT 1 FROM candidate_submissions cs2 
                WHERE cs2.job_id = r.job_id
           )))
    AND idt.interview_date_time IS NOT NULL
""", nativeQuery = true)
    long countAllInterviewsByClientName(@Param("clientName") String clientName);

    // Count ALL placements for a client across ALL job IDs
    @Query(value = """
    SELECT COUNT(*) 
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    AND (
        (JSON_VALID(idt.interview_status) 
         AND JSON_SEARCH(idt.interview_status, 'one', 'Placed', NULL, '$[*].status') IS NOT NULL)
        OR UPPER(idt.interview_status) = 'PLACED'
    )
""", nativeQuery = true)
    long countAllPlacementsByClientName(@Param("clientName") String clientName);

    // ✅ Count distinct job IDs for a given client across all job IDs
    @Query(value = """
    SELECT COUNT(*) 
    FROM (
        SELECT DISTINCT r.job_id 
        FROM requirements_model r
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        WHERE TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(:clientName)) COLLATE utf8mb4_bin
        AND r.job_id IS NOT NULL
    ) AS distinct_jobs
""", nativeQuery = true)
    long countRequirementsByClientName(@Param("clientName") String clientName);

    @Query(value = "SELECT * FROM user_details WHERE joining_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<UserDetails> findEmployeesByJoiningDateRange(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions c 
    JOIN requirements_model r ON c.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    AND c.profile_received_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllSubmissionsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*) 
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE (b.client_name = :clientName OR r.client_name = :clientName 
           OR (:clientName IS NULL AND EXISTS (
                SELECT 1 FROM candidate_submissions cs2 
                WHERE cs2.job_id = r.job_id
           )))
    AND idt.interview_date_time IS NOT NULL
    AND CAST(idt.interview_date_time AS DATE) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllInterviewsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*) 
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    AND (
        (JSON_VALID(idt.interview_status) 
         AND JSON_SEARCH(idt.interview_status, 'one', 'PLACED', NULL, '$[*].status') IS NOT NULL)
        OR UPPER(idt.interview_status) = 'PLACED'
    )
    AND CAST(idt.interview_date_time AS DATE) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllPlacementsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*) 
    FROM (
        SELECT DISTINCT r.job_id 
        FROM requirements_model r
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        WHERE TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(:clientName)) COLLATE utf8mb4_bin
        AND r.job_id IS NOT NULL
        AND CAST(r.requirement_added_time_stamp AS DATE) BETWEEN :startDate AND :endDate
    ) AS distinct_jobs
""", nativeQuery = true)
    long countRequirementsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query(value = """
    SELECT COUNT(*) 
    FROM bdm_client 
    WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId)
    AND DATE(created_at) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countClientsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT client_name 
    FROM bdm_client 
    WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId)
    AND DATE(created_at) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<String> findClientNamesByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
