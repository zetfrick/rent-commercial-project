// catalog-service/src/main/java/com/example/catalog/repository/CommentRepository.java
package com.example.catalog.repository;

import com.example.catalog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPremiseIdOrderByCreatedAtDesc(Long premiseId);

    void deleteByPremiseId(Long premiseId);

    // НОВЫЕ МЕТОДЫ ДЛЯ ОТВЕТОВ

    // Найти все корневые комментарии (не ответы)
    @Query("SELECT c FROM Comment c WHERE c.premiseId = :premiseId AND c.parentCommentId IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByPremiseId(@Param("premiseId") Long premiseId);

    // Найти все ответы на конкретный комментарий
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    // Найти все комментарии с ответами (иерархически)
    @Query("SELECT c FROM Comment c WHERE c.premiseId = :premiseId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPremiseIdOrderByCreatedAtAsc(@Param("premiseId") Long premiseId);
}