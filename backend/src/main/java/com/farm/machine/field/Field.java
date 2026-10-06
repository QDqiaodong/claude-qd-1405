package com.farm.machine.field;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/** 地块台账：合作社托管地块，按作物 / 土壤登记并跟踪耕作进度。 */
@Data
@Entity
// field 是 MySQL 函数名，统一用反引号包裹，保证建表与查询都能作为标识符使用
@Table(name = "`field`")
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 地块号，唯一 */
    @Column(unique = true)
    private String fieldNo;

    /** 地块名 */
    private String fieldName;

    /** 面积（亩），必须大于 0 */
    private BigDecimal area;

    /** 作物类型：小麦 / 玉米 / 水稻 / 大豆 / 油菜 */
    private String cropType;

    /** 土壤类型：壤土 / 砂土 / 黏土 */
    private String soilType;

    /** 状态：待耕 / 已耕 / 已播 / 已收 / 休耕 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
