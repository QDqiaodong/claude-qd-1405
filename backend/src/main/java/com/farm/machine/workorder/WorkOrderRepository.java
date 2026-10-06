package com.farm.machine.workorder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    /** 同一农机同一日期的未结订单（日期冲突校验用） */
    List<WorkOrder> findByMachineIdAndPlanDateAndStatusIn(Long machineId, LocalDate planDate, List<String> statuses);

    List<WorkOrder> findByMachineId(Long machineId);

    List<WorkOrder> findByFieldId(Long fieldId);
}
