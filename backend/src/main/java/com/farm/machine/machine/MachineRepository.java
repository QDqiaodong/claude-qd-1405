package com.farm.machine.machine;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByMachineNo(String machineNo);
}
