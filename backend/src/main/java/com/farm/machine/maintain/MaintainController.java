package com.farm.machine.maintain;

import com.farm.machine.common.ApiResponse;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintains")
public class MaintainController {

    private final MaintainService service;

    public MaintainController(MaintainService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Maintain>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Maintain> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Maintain> create(@RequestBody Maintain in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Maintain> update(@PathVariable Long id, @RequestBody Maintain in) {
        return ApiResponse.success(service.update(id, in));
    }

    /** 开始维修：待修 → 维修中，农机置维修 */
    @PostMapping("/{id}/start")
    public ApiResponse<Maintain> start(@PathVariable Long id) {
        return ApiResponse.success(service.start(id));
    }

    /** 完成维修：维修中 → 已完成，农机回待命 */
    @PostMapping("/{id}/complete")
    public ApiResponse<Maintain> complete(@PathVariable Long id) {
        return ApiResponse.success(service.complete(id));
    }
}
