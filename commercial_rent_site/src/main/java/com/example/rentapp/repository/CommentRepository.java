package com.example.rentapp.repository;

import com.example.rentapp.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPremiseIdOrderByCreatedAtDesc(Long premiseId);
}