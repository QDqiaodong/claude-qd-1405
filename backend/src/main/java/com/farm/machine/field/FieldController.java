package com.farm.machine.field;

import com.farm.machine.common.ApiResponse;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
public class FieldController {

    private final FieldService service;

    public FieldController(FieldService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Field>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Field> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Field> create(@RequestBody Field in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Field> update(@PathVariable Long id, @RequestBody Field in) {
        return ApiResponse.success(service.update(id, in));
    }

    /** 地块状态推进（待耕→已耕→已播→已收→待耕，不可跳级） */
    @PostMapping("/{id}/status")
    public ApiResponse<Field> updateStatus(@PathVariable Long id,
                                           @RequestParam(required = false) String status) {
        return ApiResponse.success(service.updateStatus(id, status));
    }
}
