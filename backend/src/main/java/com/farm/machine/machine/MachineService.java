package com.farm.machine.machine;

import com.farm.machine.common.BizException;
import com.farm.machine.common.Dict;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MachineService {

    private final MachineRepository repo;

    public MachineService(MachineRepository repo) {
        this.repo = repo;
    }

    public List<Machine> list() {
        return repo.findAll();
    }

    public Machine get(Long id) {
        if (id == null) {
            throw new BizException("农机 id 不能为空");
        }
        return repo.findById(id).orElseThrow(() -> new BizException("农机不存在"));
    }

    public Machine create(Machine in) {
        if (in == null) {
            throw new BizException("请提交农机信息");
        }
        String machineNo = in.getMachineNo();
        if (machineNo == null || machineNo.isBlank()) {
            throw new BizException("机号不能为空");
        }
        machineNo = machineNo.trim();
        if (repo.findByMachineNo(machineNo).isPresent()) {
            throw new BizException("机号已存在：" + machineNo);
        }
        if (!Dict.isValidMachineType(in.getMachineType())) {
            throw new BizException("农机类型必须是：拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机");
        }
        if (!Dict.isPositive(in.getWorkWidth())) {
            throw new BizException("作业幅宽必须大于 0");
        }
        String status = Dict.orDefault(in.getStatus(), Dict.MACHINE_IDLE);
        if (!Dict.isValidMachineStatus(status)) {
            throw new BizException("农机状态必须是：待命 / 作业中 / 维修 / 报废");
        }

        Machine m = new Machine();
        m.setMachineNo(machineNo);
        m.setMachineType(in.getMachineType());
        m.setBrand(Dict.orDefault(in.getBrand(), "未知"));
        m.setPlateNo(Dict.orDefault(in.getPlateNo(), ""));
        m.setWorkWidth(in.getWorkWidth());
        m.setPurchaseDate(in.getPurchaseDate() == null ? LocalDate.now() : in.getPurchaseDate());
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    public Machine update(Long id, Machine in) {
        Machine m = get(id);
        if (Dict.MACHINE_SCRAPPED.equals(m.getStatus())) {
            throw new BizException("农机已报废，为终态不可再修改");
        }
        if (in == null) {
            throw new BizException("请提交农机信息");
        }
        if (in.getMachineNo() != null && !in.getMachineNo().isBlank()) {
            String no = in.getMachineNo().trim();
            if (!no.equals(m.getMachineNo()) && repo.findByMachineNo(no).isPresent()) {
                throw new BizException("机号已存在：" + no);
            }
            m.setMachineNo(no);
        }
        if (in.getMachineType() != null && !in.getMachineType().isBlank()) {
            if (!Dict.isValidMachineType(in.getMachineType())) {
                throw new BizException("农机类型必须是：拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机");
            }
            m.setMachineType(in.getMachineType());
        }
        if (in.getBrand() != null && !in.getBrand().isBlank()) {
            m.setBrand(in.getBrand().trim());
        }
        if (in.getPlateNo() != null && !in.getPlateNo().isBlank()) {
            m.setPlateNo(in.getPlateNo().trim());
        }
        if (in.getWorkWidth() != null) {
            if (!Dict.isPositive(in.getWorkWidth())) {
                throw new BizException("作业幅宽必须大于 0");
            }
            m.setWorkWidth(in.getWorkWidth());
        }
        if (in.getPurchaseDate() != null) {
            m.setPurchaseDate(in.getPurchaseDate());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank()) {
            if (!Dict.canTransferMachine(m.getStatus(), in.getStatus())) {
                throw new BizException("农机状态流转非法：" + m.getStatus() + " → " + in.getStatus());
            }
            m.setStatus(in.getStatus());
        }
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    /** 派工：农机置「作业中」。只有「待命」农机可派工。 */
    public Machine startWork(Long id) {
        Machine m = get(id);
        if (!Dict.MACHINE_IDLE.equals(m.getStatus())) {
            throw new BizException("该农机当前状态为「" + m.getStatus() + "」，只有待命的农机可派工");
        }
        return changeStatus(m, Dict.MACHINE_WORKING);
    }

    /** 完工：农机回到「待命」。 */
    public Machine finishWork(Long id) {
        Machine m = get(id);
        if (!Dict.MACHINE_WORKING.equals(m.getStatus())) {
            throw new BizException("该农机当前不在作业中，无需完工");
        }
        return changeStatus(m, Dict.MACHINE_IDLE);
    }

    /** 送修：农机置「维修」。报废农机不可再维修。 */
    public Machine startRepair(Long id) {
        Machine m = get(id);
        if (Dict.MACHINE_SCRAPPED.equals(m.getStatus())) {
            throw new BizException("农机已报废，不可再送修");
        }
        return changeStatus(m, Dict.MACHINE_REPAIRING);
    }

    /** 修完：农机回到「待命」。 */
    public Machine finishRepair(Long id) {
        Machine m = get(id);
        if (!Dict.MACHINE_REPAIRING.equals(m.getStatus())) {
            throw new BizException("该农机当前不在维修中，无需修完");
        }
        return changeStatus(m, Dict.MACHINE_IDLE);
    }

    /** 报废：终态，之后不可再改。 */
    public Machine scrap(Long id) {
        Machine m = get(id);
        if (Dict.MACHINE_SCRAPPED.equals(m.getStatus())) {
            throw new BizException("农机已报废，不可重复报废");
        }
        return changeStatus(m, Dict.MACHINE_SCRAPPED);
    }

    private Machine changeStatus(Machine m, String to) {
        if (!Dict.canTransferMachine(m.getStatus(), to)) {
            throw new BizException("农机状态流转非法：" + m.getStatus() + " → " + to);
        }
        m.setStatus(to);
        m.setUpdatedAt(LocalDateTime.now());
        return repo.save(m);
    }
}
