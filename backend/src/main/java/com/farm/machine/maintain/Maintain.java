package com.farm.machine.maintain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 维修保养记录：农机的日常保养、故障维修、年检与季前检修。 */
@Data
@Entity
@Table(name = "maintain")
public class Maintain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 农机 id（外键 machine.id） */
    private Long machineId;

    /** 维修类型：日常保养 / 故障维修 / 年检 / 季前检修 */
    private String maintainType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期，可空（未完成时为 NULL） */
    private LocalDate endDate;

    /** 费用（元），不得为负 */
    private BigDecimal cost;

    /** 维修人 */
    private String mechanic;

    /** 状态：待修 / 维修中 / 已完成 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
