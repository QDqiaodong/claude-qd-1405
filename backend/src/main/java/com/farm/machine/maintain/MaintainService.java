package com.farm.machine.maintain;

import com.farm.machine.common.BizException;
import com.farm.machine.common.Dict;
import com.farm.machine.machine.Machine;
import com.farm.machine.machine.MachineService;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintainService {

    private final MaintainRepository repo;
    private final MachineService machineService;

    public MaintainService(MaintainRepository repo, MachineService machineService) {
        this.repo = repo;
        this.machineService = machineService;
    }

    public List<Maintain> list() {
        return repo.findAll();
    }

    public Maintain get(Long id) {
        if (id == null) {
            throw new BizException("维修记录 id 不能为空");
        }
        return repo.findById(id).orElseThrow(() -> new BizException("维修保养记录不存在"));
    }

    public Maintain create(Maintain in) {
        if (in == null) {
            throw new BizException("请提交维修保养信息");
        }
        Machine machine = machineService.get(in.getMachineId());
        if (Dict.MACHINE_SCRAPPED.equals(machine.getStatus())) {
            throw new BizException("农机已报废，不可再维修");
        }
        if (!Dict.isValidMaintainType(in.getMaintainType())) {
            throw new BizException("维修类型必须是：日常保养 / 故障维修 / 年检 / 季前检修");
        }
        LocalDate startDate = in.getStartDate();
        if (startDate == null) {
            throw new BizException("开始日期不能为空");
        }
        checkEndDate(startDate, in.getEndDate());
        BigDecimal cost = in.getCost() == null ? BigDecimal.ZERO : in.getCost();
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("维修费用不能为负数");
        }
        String status = Dict.orDefault(in.getStatus(), "待修");
        if (!Dict.isValidMaintainStatus(status)) {
            throw new BizException("维修状态必须是：待修 / 维修中 / 已完成");
        }

        Maintain m = new Maintain();
        m.setMachineId(machine.getId());
        m.setMaintainType(in.getMaintainType());
        m.setStartDate(startDate);
        m.setEndDate(in.getEndDate());
        m.setCost(cost);
        m.setMechanic(Dict.orDefault(in.getMechanic(), "待定"));
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    public Maintain update(Long id, Maintain in) {
        Maintain m = get(id);
        if (Dict.MAINTAIN_DONE.equals(m.getStatus())) {
            throw new BizException("已完成的维修记录不可再修改");
        }
        if (in == null) {
            throw new BizException("请提交维修保养信息");
        }
        if (in.getMachineId() != null && !in.getMachineId().equals(m.getMachineId())) {
            Machine machine = machineService.get(in.getMachineId());
            if (Dict.MACHINE_SCRAPPED.equals(machine.getStatus())) {
                throw new BizException("农机已报废，不可再维修");
            }
            m.setMachineId(in.getMachineId());
        }
        if (in.getMaintainType() != null && !in.getMaintainType().isBlank()) {
            if (!Dict.isValidMaintainType(in.getMaintainType())) {
                throw new BizException("维修类型必须是：日常保养 / 故障维修 / 年检 / 季前检修");
            }
            m.setMaintainType(in.getMaintainType());
        }
        if (in.getStartDate() != null) {
            m.setStartDate(in.getStartDate());
        }
        if (in.getEndDate() != null) {
            m.setEndDate(in.getEndDate());
        }
        checkEndDate(m.getStartDate(), m.getEndDate());
        if (in.getCost() != null) {
            if (in.getCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("维修费用不能为负数");
            }
            m.setCost(in.getCost());
        }
        if (in.getMechanic() != null && !in.getMechanic().isBlank()) {
            m.setMechanic(in.getMechanic().trim());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank()) {
            if (!Dict.isValidMaintainStatus(in.getStatus())) {
                throw new BizException("维修状态必须是：待修 / 维修中 / 已完成");
            }
            m.setStatus(in.getStatus());
        }
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    /** 开始维修：记录置「维修中」，农机置「维修」。 */
    public Maintain start(Long id) {
        Maintain m = get(id);
        if (!"待修".equals(m.getStatus())) {
            throw new BizException("只有待修的维修记录可以开始维修");
        }
        machineService.startRepair(m.getMachineId());
        m.setStatus("维修中");
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    /** 完成维修：记录置「已完成」，农机回「待命」；结束日期缺省为今天。 */
    public Maintain complete(Long id) {
        Maintain m = get(id);
        if (!"维修中".equals(m.getStatus())) {
            throw new BizException("只有维修中的记录可以完成");
        }
        LocalDate endDate = m.getEndDate() == null ? LocalDate.now() : m.getEndDate();
        if (endDate.isBefore(m.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        machineService.finishRepair(m.getMachineId());
        m.setEndDate(endDate);
        m.setStatus(Dict.MAINTAIN_DONE);
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    private void checkEndDate(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BizException("结束日期不能早于开始日期");
        }
    }
}
