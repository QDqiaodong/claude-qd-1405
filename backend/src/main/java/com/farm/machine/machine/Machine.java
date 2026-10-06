package com.farm.machine.machine;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 农机台账：合作社名下拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机。 */
@Data
@Entity
@Table(name = "machine")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 机号，唯一 */
    @Column(unique = true)
    private String machineNo;

    /** 农机类型：拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机 */
    private String machineType;

    /** 品牌 */
    private String brand;

    /** 牌号（农机牌照） */
    private String plateNo;

    /** 作业幅宽（米），必须大于 0 */
    private BigDecimal workWidth;

    /** 购入日期 */
    private LocalDate purchaseDate;

    /** 状态：待命 / 作业中 / 维修 / 报废 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
