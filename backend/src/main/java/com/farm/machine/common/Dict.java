package com.farm.machine.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 农机合作社业务词典与规则参数表：农机类型 / 农机状态流转、地块作物 / 土壤 / 耕作状态流转、
 * 作业订单作业类型 / 状态流转（含作业类型对地块状态的推进）、维修保养类型 / 状态流转。
 * 规则集中在此，便于出题时被要求调整或扩展。
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 农机 machine ----------------
    /** 拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机 */
    public static final List<String> MACHINE_TYPE = List.of("拖拉机", "收割机", "播种机", "旋耕机", "植保机");

    /** 待命 / 作业中 / 维修 / 报废 */
    public static final List<String> MACHINE_STATUS = List.of("待命", "作业中", "维修", "报废");

    /** 可派工的农机状态：只有「待命」可派工 */
    public static final String MACHINE_IDLE = "待命";
    public static final String MACHINE_WORKING = "作业中";
    public static final String MACHINE_REPAIRING = "维修";
    public static final String MACHINE_SCRAPPED = "报废";

    /** 农机状态机：待命→作业中 / 维修 / 报废；作业中→待命 / 维修；维修→待命 / 报废；报废为终态不可再改 */
    public static final Map<String, List<String>> MACHINE_FLOW = Map.of(
            "待命", List.of("作业中", "维修", "报废"),
            "作业中", List.of("待命", "维修"),
            "维修", List.of("待命", "报废"),
            "报废", List.of());

    // ---------------- 地块 field ----------------
    /** 小麦 / 玉米 / 水稻 / 大豆 / 油菜 */
    public static final List<String> FIELD_CROP = List.of("小麦", "玉米", "水稻", "大豆", "油菜");

    /** 壤土 / 砂土 / 黏土 */
    public static final List<String> FIELD_SOIL = List.of("壤土", "砂土", "黏土");

    /** 待耕 / 已耕 / 已播 / 已收 / 休耕 */
    public static final List<String> FIELD_STATUS = List.of("待耕", "已耕", "已播", "已收", "休耕");

    /** 休耕地块不可派作业 */
    public static final String FIELD_FALLOW = "休耕";

    /** 地块状态机：待耕→已耕→已播→已收→待耕（新一轮），不可跳级；休耕↔待耕 */
    public static final Map<String, List<String>> FIELD_FLOW = Map.of(
            "待耕", List.of("已耕", "休耕"),
            "已耕", List.of("已播"),
            "已播", List.of("已收"),
            "已收", List.of("待耕"),
            "休耕", List.of("待耕"));

    // ---------------- 作业订单 workorder ----------------
    /** 耕地 / 播种 / 施肥 / 植保 / 收割 */
    public static final List<String> WORK_TYPE = List.of("耕地", "播种", "施肥", "植保", "收割");

    /** 待派工 / 作业中 / 已完成 / 已取消 */
    public static final List<String> WORK_STATUS = List.of("待派工", "作业中", "已完成", "已取消");

    /** 已完成 / 已取消 为终态不可再改 */
    public static final List<String> WORK_FINAL_STATUS = List.of("已完成", "已取消");

    /** 同一农机同一日期只允许一个「待派工」或「作业中」订单 */
    public static final List<String> WORK_ACTIVE_STATUS = List.of("待派工", "作业中");

    /** 作业类型完工后推进的地块状态：耕地→已耕、播种→已播、收割→已收（施肥 / 植保不改变地块状态） */
    public static final Map<String, String> FIELD_STATUS_BY_WORK = Map.of(
            "耕地", "已耕",
            "播种", "已播",
            "收割", "已收");

    // ---------------- 维修保养 maintain ----------------
    /** 日常保养 / 故障维修 / 年检 / 季前检修 */
    public static final List<String> MAINTAIN_TYPE = List.of("日常保养", "故障维修", "年检", "季前检修");

    /** 待修 / 维修中 / 已完成 */
    public static final List<String> MAINTAIN_STATUS = List.of("待修", "维修中", "已完成");

    /** 已完成为终态不可再改 */
    public static final String MAINTAIN_DONE = "已完成";

    // ---------------- 取值方法 ----------------

    public static boolean isValidMachineType(String t) {
        return t != null && MACHINE_TYPE.contains(t);
    }

    public static boolean isValidMachineStatus(String s) {
        return s != null && MACHINE_STATUS.contains(s);
    }

    public static boolean isValidFieldCrop(String c) {
        return c != null && FIELD_CROP.contains(c);
    }

    public static boolean isValidFieldSoil(String s) {
        return s != null && FIELD_SOIL.contains(s);
    }

    public static boolean isValidFieldStatus(String s) {
        return s != null && FIELD_STATUS.contains(s);
    }

    public static boolean isValidWorkType(String t) {
        return t != null && WORK_TYPE.contains(t);
    }

    public static boolean isValidWorkStatus(String s) {
        return s != null && WORK_STATUS.contains(s);
    }

    public static boolean isValidMaintainType(String t) {
        return t != null && MAINTAIN_TYPE.contains(t);
    }

    public static boolean isValidMaintainStatus(String s) {
        return s != null && MAINTAIN_STATUS.contains(s);
    }

    /** 农机状态流转校验：报废为终态不可再改，其余按 MACHINE_FLOW 推进。 */
    public static boolean canTransferMachine(String from, String to) {
        if (!isValidMachineStatus(from) || !isValidMachineStatus(to)) {
            return false;
        }
        List<String> next = MACHINE_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /** 地块状态流转校验：待耕→已耕→已播→已收→待耕，不可跳级。 */
    public static boolean canTransferField(String from, String to) {
        if (!isValidFieldStatus(from) || !isValidFieldStatus(to)) {
            return false;
        }
        List<String> next = FIELD_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /** 作业订单是否处于终态（已完成 / 已取消）。 */
    public static boolean isWorkFinal(String status) {
        return status != null && WORK_FINAL_STATUS.contains(status);
    }

    /** 空白串兜底。 */
    public static String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    /** 正数校验：非 null 且严格大于 0。 */
    public static boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }
}
