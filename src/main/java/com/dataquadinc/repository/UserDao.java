package com.dataquadinc.repository;

import com.dataquadinc.dto.UserDto;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserDao extends JpaRepository<UserDetails, String>, JpaSpecificationExecutor<UserDetails> {

    // ============ EXISTING METHODS ============

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' AND u.designation = 'Candidate'")
    List<UserDetails> findAllActiveExternalUser();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' AND u.designation <> 'Candidate'")
    List<UserDetails> findAllActiveNotExternalUser();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'INACTIVE' AND u.designation = 'Candidate'")
    List<UserDetails> findAllInActiveExternalUser();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'INACTIVE' AND u.designation <> 'Candidate'")
    List<UserDetails> findAllInActiveNotExternalUser();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' AND u.designation <> 'testuser'")
    List<UserDetails> findAllActiveNonTestUsers();

    UserDetails findByEmail(String email);

    UserDetails findByUserId(String userId);

    @Query("SELECT DISTINCT u FROM UserDetails u JOIN u.roles r " +
            "WHERE (:userId IS NULL OR u.userId = :userId) " +
            "AND (:roleEnum IS NULL OR r.name = :roleEnum) " +
            "AND u.entity = :entity " +
            "AND u.status = 'ACTIVE' AND u.designation <> 'testuser'")
    List<UserDetails> findByUserIdAndRole(@Param("userId") String userId,
                                          @Param("roleEnum") UserType roleEnum,
                                          @Param("entity") String entity);

    @Query("SELECT DISTINCT u FROM UserDetails u " +
            "WHERE (:userId IS NULL OR u.userId = :userId) " +
            "AND u.entity = :entity " +
            "AND u.status = 'ACTIVE' AND u.designation <> 'testuser' " +
            "AND (:excludeRole IS NULL OR NOT EXISTS (" +
            "   SELECT 1 FROM u.roles r2 WHERE r2.name = :excludeRole" +
            "))")
    List<UserDetails> findByUserIdAndRoleNot(@Param("userId") String userId,
                                             @Param("excludeRole") UserType excludeRole,
                                             @Param("entity") String entity);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = :entity " +
            "AND u.status = 'ACTIVE' AND u.designation <> 'testuser'")
    List<UserDetails> findAllActiveNonTestUsersByEntity(@Param("entity") String entity);

    @Query("SELECT u FROM UserDetails u JOIN u.roles r WHERE r.name = 'BDM'")
    List<UserDetails> findBdmEmployees();

    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions cs
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    """, nativeQuery = true)
    long countAllSubmissionsAcrossAllJobsAndClients();

    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions cs
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    """, nativeQuery = true)
    long countAllSubmissionsByClientName(@Param("clientName") String clientName);

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
    """, nativeQuery = true)
    long countAllPlacementsByClientName(@Param("clientName") String clientName);

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
         AND JSON_SEARCH(idt.interview_status, 'one', 'Placed', NULL, '$[*].status') IS NOT NULL)
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

    @Query("""
        SELECT u
        FROM UserDetails u
        WHERE u.entity = 'IN'
        AND u.status = 'ACTIVE'
        AND u.designation <> 'testuser'
        ORDER BY u.userId
        """)
    List<UserDetails> findAllAttendanceEmployees();

    @Query("""
        SELECT u.userName
        FROM UserDetails u
        WHERE u.userId = :teamLeadId
        """)
    String getTeamLeadName(
            @Param("teamLeadId") String teamLeadId
    );

    @Query("""
       SELECT u
       FROM UserDetails u
       WHERE u.status = 'ACTIVE'
       """)
    List<UserDetails> findAllActiveUsers();

    List<UserDetails> findByAssociatedTeamLeadId(String teamLeadId);

    List<UserDetails> findByUserIdIn(List<String> userIds);

    Page<UserDetails> findAll(Specification<UserDetails> spec, Pageable pageable);

    // ============ NEW METHODS FOR ATTENDANCE SYSTEM ============

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser'")
    List<UserDetails> findAllActiveInEmployeesExcludingExternal();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE'")
    List<UserDetails> findAllActiveInEmployees();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser' " +
            "AND u.department = :department")
    List<UserDetails> findActiveInEmployeesByDepartment(@Param("department") String department);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser' " +
            "AND u.reportingManager = :managerId")
    List<UserDetails> findActiveInEmployeesByReportingManager(@Param("managerId") String managerId);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser' " +
            "AND u.designation = :designation")
    List<UserDetails> findActiveInEmployeesByDesignation(@Param("designation") String designation);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser'")
    Page<UserDetails> findActiveInEmployeesWithPagination(Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser'")
    long countActiveInEmployeesExcludingExternal();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser' " +
            "AND u.joiningDate BETWEEN :startDate AND :endDate")
    List<UserDetails> findActiveInEmployeesByJoiningDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser' " +
            "AND u.isEmployeeHavingPF = :hasPF " +
            "AND u.isEmployeeHavingESI = :hasESI")
    List<UserDetails> findActiveInEmployeesByPfEsiStatus(
            @Param("hasPF") Boolean hasPF,
            @Param("hasESI") Boolean hasESI);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.userId = :userId " +
            "AND u.entity = 'IN' " +
            "AND u.status = 'ACTIVE'")
    UserDetails findActiveInEmployeeByUserId(@Param("userId") String userId);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.userId IN :userIds " +
            "AND u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate'")
    List<UserDetails> findActiveInEmployeesByUserIds(@Param("userIds") List<String> userIds);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity IN :entities " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.designation <> 'testuser'")
    List<UserDetails> findAllActiveEmployeesByEntities(@Param("entities") List<String> entities);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND (LOWER(u.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(u.userId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<UserDetails> searchActiveInEmployees(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.probation = 'YES'")
    List<UserDetails> findActiveInEmployeesOnProbation();

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "AND u.teamName = :teamName")
    List<UserDetails> findActiveInEmployeesByTeam(@Param("teamName") String teamName);

    @Query("SELECT u.department, COUNT(u) FROM UserDetails u " +
            "WHERE u.entity = 'IN' " +
            "AND u.status = 'ACTIVE' " +
            "AND u.designation <> 'Candidate' " +
            "GROUP BY u.department")
    List<Object[]> getEmployeeCountByDepartment();

    // ============ OPTIMIZED METHODS FOR BULK ATTENDANCE ============

    /**
     * Paginated active IN employees with search across name, userId, AND reportingManager.
     * Used by the server-side paginated bulk attendance endpoint.
     *
     * search  – matched against userName, userId, and reportingManager (case-insensitive LIKE)
     * department – exact match filter (optional)
     */
    @Query("""
            SELECT u FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            AND (:search IS NULL OR 
                 LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.userId)   LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.reportingManager) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:department IS NULL OR u.department = :department)
            """)
    Page<UserDetails> findActiveInEmployeesWithFilters(
            @Param("search") String search,
            @Param("department") String department,
            Pageable pageable);

    @Query("""
            SELECT u.userId FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            """)
    List<String> findAllActiveInEmployeeIds();

    @Query("""
            SELECT u.userId, u.userName, u.designation, u.department 
            FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            ORDER BY u.userName
            """)
    List<Object[]> findActiveEmployeeProjection();

    @Query("""
            SELECT u FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            AND (:department IS NULL OR u.department = :department)
            """)
    Page<UserDetails> findActiveInEmployeesByDepartmentWithPagination(
            @Param("department") String department,
            Pageable pageable);

    @Query("""
            SELECT u FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            AND (:search IS NULL OR 
                 LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR 
                 LOWER(u.userId) LIKE LOWER(CONCAT('%', :search, '%')) OR 
                 LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<UserDetails> searchActiveEmployeesWithPagination(
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT u.department, COUNT(u) 
            FROM UserDetails u 
            WHERE u.entity = 'IN' 
            AND u.status = 'ACTIVE' 
            AND u.designation <> 'Candidate' 
            AND u.designation <> 'testuser'
            AND (:search IS NULL OR 
                 LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR 
                 LOWER(u.userId) LIKE LOWER(CONCAT('%', :search, '%')))
            GROUP BY u.department
            """)
    List<Object[]> getEmployeeCountByDepartmentWithSearch(
            @Param("search") String search);
}