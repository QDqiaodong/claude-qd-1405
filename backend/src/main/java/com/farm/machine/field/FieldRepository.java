package com.farm.machine.field;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Long> {

    Optional<Field> findByFieldNo(String fieldNo);
}
