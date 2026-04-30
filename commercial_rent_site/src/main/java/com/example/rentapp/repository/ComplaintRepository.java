package com.example.rentapp.repository;

import com.example.rentapp.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByResolvedOrderByCreatedAtDesc(boolean resolved);

    List<Complaint> findByStatusAndResolvedOrderByCreatedAtDesc(String status, boolean resolved);

    List<Complaint> findByStatusOrderByCreatedAtDesc(String status);

    @Modifying
    @Transactional
    @Query("UPDATE Complaint c SET c.resolved = true, c.resolvedBy = :adminId, c.resolvedByName = :adminName, c.resolvedAt = CURRENT_TIMESTAMP, c.status = 'RESOLVED' WHERE c.id = :complaintId")
    void resolveComplaint(@Param("complaintId") Long complaintId, @Param("adminId") Long adminId, @Param("adminName") String adminName);

    @Modifying
    @Transactional
    @Query("UPDATE Complaint c SET c.status = 'IN_WORK' WHERE c.id = :complaintId")
    void takeWorkComplaint(@Param("complaintId") Long complaintId);

    @Modifying
    @Transactional
    @Query("UPDATE Complaint c SET c.status = 'ACTIVE' WHERE c.id = :complaintId")
    void rejectComplaint(@Param("complaintId") Long complaintId);

    List<Complaint> findByResolvedTrueAndResolvedAtBefore(LocalDateTime date);
}