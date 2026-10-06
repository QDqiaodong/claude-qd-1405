package com.farm.machine.workorder;

import com.farm.machine.common.ApiResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WorkOrder>> list() {
        return ApiResponse.success(service.list());
    }

    /** 甘特区间查询：/api/work-orders/gantt?from=2024-05-01&to=2024-05-14 */
    @GetMapping("/gantt")
    public ApiResponse<List<WorkOrder>> gantt(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(service.listByRange(from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrder> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<WorkOrder> create(@RequestBody WorkOrder in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkOrder> update(@PathVariable Long id, @RequestBody WorkOrder in) {
        return ApiResponse.success(service.update(id, in));
    }

    /** 开工：待派工 → 作业中，农机置作业中 */
    @PostMapping("/{id}/start")
    public ApiResponse<WorkOrder> start(@PathVariable Long id) {
        return ApiResponse.success(service.start(id));
    }

    /** 完工：作业中 → 已完成，农机回待命并推进地块状态 */
    @PostMapping("/{id}/complete")
    public ApiResponse<WorkOrder> complete(@PathVariable Long id) {
        return ApiResponse.success(service.complete(id));
    }

    /** 取消：→ 已取消（终态） */
    @PostMapping("/{id}/cancel")
    public ApiResponse<WorkOrder> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(id));
    }
}
