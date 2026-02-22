package com.nexaedi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetterEntry, UUID> {
}
