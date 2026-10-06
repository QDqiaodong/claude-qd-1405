package com.farm.machine.workorder;

import com.farm.machine.common.BizException;
import com.farm.machine.common.Dict;
import com.farm.machine.field.Field;
import com.farm.machine.field.FieldService;
import com.farm.machine.machine.Machine;
import com.farm.machine.machine.MachineService;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository repo;
    private final MachineService machineService;
    private final FieldService fieldService;

    public WorkOrderService(WorkOrderRepository repo, MachineService machineService, FieldService fieldService) {
        this.repo = repo;
        this.machineService = machineService;
        this.fieldService = fieldService;
    }

    public List<WorkOrder> list() {
        return repo.findAll();
    }

    /** 甘特视图：指定日期区间内的订单。 */
    public List<WorkOrder> listByRange(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(7) : to;
        if (end.isBefore(start)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        return repo.findAll().stream()
                .filter(w -> w.getPlanDate() != null
                        && !w.getPlanDate().isBefore(start)
                        && !w.getPlanDate().isAfter(end))
                .toList();
    }

    public WorkOrder get(Long id) {
        if (id == null) {
            throw new BizException("作业订单 id 不能为空");
        }
        return repo.findById(id).orElseThrow(() -> new BizException("作业订单不存在"));
    }

    public WorkOrder create(WorkOrder in) {
        if (in == null) {
            throw new BizException("请提交作业订单信息");
        }
        Machine machine = machineService.get(in.getMachineId());
        Field field = fieldService.get(in.getFieldId());

        if (!Dict.MACHINE_IDLE.equals(machine.getStatus())) {
            throw new BizException("该农机当前状态为「" + machine.getStatus() + "」，只有待命的农机可派工");
        }
        if (Dict.FIELD_FALLOW.equals(field.getStatus())) {
            throw new BizException("地块「" + field.getFieldName() + "」处于休耕，不可派作业");
        }
        if (!Dict.isValidWorkType(in.getWorkType())) {
            throw new BizException("作业类型必须是：耕地 / 播种 / 施肥 / 植保 / 收割");
        }
        LocalDate planDate = in.getPlanDate();
        if (planDate == null) {
            throw new BizException("计划作业日期不能为空");
        }
        if (!Dict.isPositive(in.getWorkArea())) {
            throw new BizException("作业亩数必须大于 0");
        }
        checkWorkArea(in.getWorkArea(), field);
        checkFieldJump(field, in.getWorkType());
        checkDateConflict(in.getMachineId(), planDate, null);

        WorkOrder w = new WorkOrder();
        w.setMachineId(machine.getId());
        w.setFieldId(field.getId());
        w.setOperator(Dict.orDefault(in.getOperator(), "待定"));
        w.setWorkType(in.getWorkType());
        w.setPlanDate(planDate);
        w.setWorkArea(in.getWorkArea());
        w.setFuel(in.getFuel() == null ? BigDecimal.ZERO : in.getFuel());
        w.setStatus("待派工");
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    public WorkOrder update(Long id, WorkOrder in) {
        WorkOrder w = get(id);
        if (Dict.isWorkFinal(w.getStatus())) {
            throw new BizException("已完成或已取消的作业订单为终态，不可再修改");
        }
        if (in == null) {
            throw new BizException("请提交作业订单信息");
        }
        if (in.getMachineId() != null && !in.getMachineId().equals(w.getMachineId())) {
            Machine machine = machineService.get(in.getMachineId());
            if (!Dict.MACHINE_IDLE.equals(machine.getStatus())) {
                throw new BizException("该农机当前状态为「" + machine.getStatus() + "」，只有待命的农机可派工");
            }
            w.setMachineId(in.getMachineId());
        }
        if (in.getFieldId() != null && !in.getFieldId().equals(w.getFieldId())) {
            Field field = fieldService.get(in.getFieldId());
            if (Dict.FIELD_FALLOW.equals(field.getStatus())) {
                throw new BizException("地块「" + field.getFieldName() + "」处于休耕，不可派作业");
            }
            w.setFieldId(in.getFieldId());
        }
        if (in.getWorkType() != null && !in.getWorkType().isBlank()) {
            if (!Dict.isValidWorkType(in.getWorkType())) {
                throw new BizException("作业类型必须是：耕地 / 播种 / 施肥 / 植保 / 收割");
            }
            w.setWorkType(in.getWorkType());
        }
        if (in.getOperator() != null && !in.getOperator().isBlank()) {
            w.setOperator(in.getOperator().trim());
        }
        if (in.getPlanDate() != null) {
            w.setPlanDate(in.getPlanDate());
        }
        if (in.getWorkArea() != null) {
            if (!Dict.isPositive(in.getWorkArea())) {
                throw new BizException("作业亩数必须大于 0");
            }
            w.setWorkArea(in.getWorkArea());
        }
        if (in.getFuel() != null) {
            if (in.getFuel().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("油耗不能为负数");
            }
            w.setFuel(in.getFuel());
        }
        checkWorkArea(w.getWorkArea(), fieldService.get(w.getFieldId()));
        checkFieldJump(fieldService.get(w.getFieldId()), w.getWorkType());
        checkDateConflict(w.getMachineId(), w.getPlanDate(), w.getId());
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    /** 开工：订单置「作业中」，农机置「作业中」。 */
    public WorkOrder start(Long id) {
        WorkOrder w = get(id);
        if (!"待派工".equals(w.getStatus())) {
            throw new BizException("只有待派工的订单可以开工");
        }
        machineService.startWork(w.getMachineId());
        w.setStatus("作业中");
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    /** 完工：订单置「已完成」，农机回「待命」，并按作业类型推进地块状态。 */
    public WorkOrder complete(Long id) {
        WorkOrder w = get(id);
        if (!"作业中".equals(w.getStatus())) {
            throw new BizException("只有作业中的订单可以完工");
        }
        Field field = fieldService.get(w.getFieldId());
        String next = Dict.FIELD_STATUS_BY_WORK.get(w.getWorkType());
        if (next != null && !Dict.canTransferField(field.getStatus(), next)) {
            throw new BizException("地块状态不可跳级：" + field.getStatus() + " → " + next
                    + "，须按 待耕→已耕→已播→已收 推进");
        }
        machineService.finishWork(w.getMachineId());
        if (next != null) {
            fieldService.advanceByWork(w.getFieldId(), w.getWorkType());
        }
        w.setStatus("已完成");
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    /** 取消：订单置「已取消」；若已开工则农机回「待命」。 */
    public WorkOrder cancel(Long id) {
        WorkOrder w = get(id);
        if (Dict.isWorkFinal(w.getStatus())) {
            throw new BizException("已完成或已取消的作业订单为终态，不可再变更");
        }
        if ("作业中".equals(w.getStatus())) {
            machineService.finishWork(w.getMachineId());
        }
        w.setStatus("已取消");
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    private void checkWorkArea(BigDecimal workArea, Field field) {
        if (workArea != null && field.getArea() != null && workArea.compareTo(field.getArea()) > 0) {
            throw new BizException("作业亩数不得超过地块面积（" + field.getArea() + " 亩）");
        }
    }

    private void checkFieldJump(Field field, String workType) {
        String next = Dict.FIELD_STATUS_BY_WORK.get(workType);
        if (next != null && !Dict.canTransferField(field.getStatus(), next)) {
            throw new BizException("地块状态不可跳级：地块当前「" + field.getStatus() + "」，"
                    + workType + "作业将推进到「" + next + "」");
        }
    }

    private void checkDateConflict(Long machineId, LocalDate planDate, Long selfId) {
        if (machineId == null || planDate == null) {
            return;
        }
        boolean conflict = repo.findByMachineIdAndPlanDateAndStatusIn(machineId, planDate, Dict.WORK_ACTIVE_STATUS)
                .stream()
                .anyMatch(o -> selfId == null || !selfId.equals(o.getId()));
        if (conflict) {
            throw new BizException("该农机在 " + planDate + " 已存在待派工或作业中的订单，日期冲突");
        }
    }
}
