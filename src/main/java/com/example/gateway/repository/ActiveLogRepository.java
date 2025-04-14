package com.example.gateway.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gateway.entity.ActiveLogEntity;


@Repository
public interface ActiveLogRepository extends JpaRepository<ActiveLogEntity, Long> {
}