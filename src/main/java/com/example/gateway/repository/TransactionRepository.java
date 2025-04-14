package com.example.gateway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gateway.entity.TransactionEntity;


@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
   
}
