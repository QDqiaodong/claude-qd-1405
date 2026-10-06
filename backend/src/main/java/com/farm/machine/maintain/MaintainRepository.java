package com.farm.machine.maintain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintainRepository extends JpaRepository<Maintain, Long> {

    List<Maintain> findByMachineId(Long machineId);
}
