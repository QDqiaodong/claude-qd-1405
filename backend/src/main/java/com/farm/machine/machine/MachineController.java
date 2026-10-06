package com.farm.machine.machine;

import com.farm.machine.common.ApiResponse;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService service;

    public MachineController(MachineService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Machine>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Machine> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Machine> create(@RequestBody Machine in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Machine> update(@PathVariable Long id, @RequestBody Machine in) {
        return ApiResponse.success(service.update(id, in));
    }

    /** 派工：待命 → 作业中 */
    @PostMapping("/{id}/dispatch")
    public ApiResponse<Machine> dispatch(@PathVariable Long id) {
        return ApiResponse.success(service.startWork(id));
    }

    /** 完工：作业中 → 待命 */
    @PostMapping("/{id}/finish")
    public ApiResponse<Machine> finish(@PathVariable Long id) {
        return ApiResponse.success(service.finishWork(id));
    }

    /** 送修：→ 维修 */
    @PostMapping("/{id}/send-repair")
    public ApiResponse<Machine> sendRepair(@PathVariable Long id) {
        return ApiResponse.success(service.startRepair(id));
    }

    /** 修完：维修 → 待命 */
    @PostMapping("/{id}/repair-done")
    public ApiResponse<Machine> repairDone(@PathVariable Long id) {
        return ApiResponse.success(service.finishRepair(id));
    }

    /** 报废：终态 */
    @PostMapping("/{id}/scrap")
    public ApiResponse<Machine> scrap(@PathVariable Long id) {
        return ApiResponse.success(service.scrap(id));
    }
}
