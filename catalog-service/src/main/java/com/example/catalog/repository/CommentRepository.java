package com.example.catalog.repository;

import com.example.catalog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPremiseIdOrderByCreatedAtDesc(Long premiseId);
}