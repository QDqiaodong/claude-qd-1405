package com.farm.machine.field;

import com.farm.machine.common.BizException;
import com.farm.machine.common.Dict;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FieldService {

    private final FieldRepository repo;

    public FieldService(FieldRepository repo) {
        this.repo = repo;
    }

    public List<Field> list() {
        return repo.findAll();
    }

    public Field get(Long id) {
        if (id == null) {
            throw new BizException("地块 id 不能为空");
        }
        return repo.findById(id).orElseThrow(() -> new BizException("地块不存在"));
    }

    public Field create(Field in) {
        if (in == null) {
            throw new BizException("请提交地块信息");
        }
        String fieldNo = in.getFieldNo();
        if (fieldNo == null || fieldNo.isBlank()) {
            throw new BizException("地块号不能为空");
        }
        fieldNo = fieldNo.trim();
        if (repo.findByFieldNo(fieldNo).isPresent()) {
            throw new BizException("地块号已存在：" + fieldNo);
        }
        if (!Dict.isPositive(in.getArea())) {
            throw new BizException("地块面积必须大于 0");
        }
        if (!Dict.isValidFieldCrop(in.getCropType())) {
            throw new BizException("作物类型必须是：小麦 / 玉米 / 水稻 / 大豆 / 油菜");
        }
        if (!Dict.isValidFieldSoil(in.getSoilType())) {
            throw new BizException("土壤类型必须是：壤土 / 砂土 / 黏土");
        }
        String status = Dict.orDefault(in.getStatus(), "待耕");
        if (!Dict.isValidFieldStatus(status)) {
            throw new BizException("地块状态必须是：待耕 / 已耕 / 已播 / 已收 / 休耕");
        }

        Field f = new Field();
        f.setFieldNo(fieldNo);
        f.setFieldName(Dict.orDefault(in.getFieldName(), fieldNo));
        f.setArea(in.getArea());
        f.setCropType(in.getCropType());
        f.setSoilType(in.getSoilType());
        f.setStatus(status);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        return repo.save(f);
    }

    public Field update(Long id, Field in) {
        Field f = get(id);
        if (in == null) {
            throw new BizException("请提交地块信息");
        }
        if (in.getFieldNo() != null && !in.getFieldNo().isBlank()) {
            String no = in.getFieldNo().trim();
            if (!no.equals(f.getFieldNo()) && repo.findByFieldNo(no).isPresent()) {
                throw new BizException("地块号已存在：" + no);
            }
            f.setFieldNo(no);
        }
        if (in.getFieldName() != null && !in.getFieldName().isBlank()) {
            f.setFieldName(in.getFieldName().trim());
        }
        if (in.getArea() != null) {
            if (!Dict.isPositive(in.getArea())) {
                throw new BizException("地块面积必须大于 0");
            }
            f.setArea(in.getArea());
        }
        if (in.getCropType() != null && !in.getCropType().isBlank()) {
            if (!Dict.isValidFieldCrop(in.getCropType())) {
                throw new BizException("作物类型必须是：小麦 / 玉米 / 水稻 / 大豆 / 油菜");
            }
            f.setCropType(in.getCropType());
        }
        if (in.getSoilType() != null && !in.getSoilType().isBlank()) {
            if (!Dict.isValidFieldSoil(in.getSoilType())) {
                throw new BizException("土壤类型必须是：壤土 / 砂土 / 黏土");
            }
            f.setSoilType(in.getSoilType());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank()) {
            f.setStatus(changeStatus(f, in.getStatus()));
        }
        f.setUpdatedAt(LocalDateTime.now());
        return repo.save(f);
    }

    /** 状态推进：待耕→已耕→已播→已收→待耕，不可跳级。 */
    public Field updateStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new BizException("地块状态不能为空");
        }
        Field f = get(id);
        f.setStatus(changeStatus(f, status.trim()));
        f.setUpdatedAt(LocalDateTime.now());
        return repo.save(f);
    }

    /** 作业完工后按作业类型推进地块状态（耕地→已耕 / 播种→已播 / 收割→已收）。 */
    public Field advanceByWork(Long id, String workType) {
        String next = Dict.FIELD_STATUS_BY_WORK.get(workType);
        if (next == null) {
            return null;
        }
        Field f = get(id);
        f.setStatus(changeStatus(f, next));
        f.setUpdatedAt(LocalDateTime.now());
        return repo.save(f);
    }

    private String changeStatus(Field f, String to) {
        if (!Dict.canTransferField(f.getStatus(), to)) {
            throw new BizException("地块状态不可跳级：" + f.getStatus() + " → " + to + "，须按 待耕→已耕→已播→已收 推进");
        }
        return to;
    }
}
