package com.farm.machine.workorder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 作业订单：把某台农机派到某块地，在计划日期完成指定作业类型的作业。 */
@Data
@Entity
@Table(name = "workorder")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 农机 id（外键 machine.id） */
    private Long machineId;

    /** 地块 id（外键 field.id） */
    private Long fieldId;

    /** 农机手 */
    private String operator;

    /** 作业类型：耕地 / 播种 / 施肥 / 植保 / 收割 */
    private String workType;

    /** 计划作业日期 */
    private LocalDate planDate;

    /** 作业亩数，必须大于 0 且不得超过地块面积 */
    private BigDecimal workArea;

    /** 油耗（升） */
    private BigDecimal fuel;

    /** 状态：待派工 / 作业中 / 已完成 / 已取消 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
