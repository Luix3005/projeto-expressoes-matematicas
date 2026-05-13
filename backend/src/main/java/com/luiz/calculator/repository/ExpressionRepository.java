package com.luiz.calculator.repository;

import com.luiz.calculator.model.Expression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;

public interface ExpressionRepository extends JpaRepository<Expression, Long>, JpaSpecificationExecutor<Expression> {

    Page<Expression> findByExpressionContainingIgnoreCaseOrCreatedByContainingIgnoreCase(
        String expression, String createdBy, Pageable pageable);

    
    Page<Expression> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

}
