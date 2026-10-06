<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  season: { type: String, default: '夏管' },
  focusMachineId: { type: Number, default: null },
  focusCrop: { type: String, default: null }
})
const emit = defineEmits(['changed'])

const list = ref([])
const machines = ref([])
const dialog = ref(false)
const editing = ref(null)

const types = ['日常保养', '故障维修', '年检', '季前检修']
const statuses = ['待修', '维修中', '已完成']

const emptyForm = () => ({
  machineId: null,
  maintainType: '日常保养',
  startDate: null,
  endDate: null,
  cost: 0,
  mechanic: ''
})
const form = ref(emptyForm())

const filtered = computed(() => {
  if (!props.focusMachineId) return list.value
  return list.value.filter(m => m.machineId === props.focusMachineId)
})

const machineLabel = (id) => {
  const m = machines.value.find(x => x.id === id)
  return m ? `${m.machineNo} ${m.machineType}（${m.status}）` : `#${id}`
}
const totalCost = computed(() => filtered.value.reduce((s, m) => s + Number(m.cost || 0), 0))

async function load() {
  const [m, ms] = await Promise.all([api.listMaintains(), api.listMachines()])
  list.value = m
  machines.value = ms
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  form.value.startDate = new Date().toISOString().slice(0, 10)
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    machineId: r.machineId,
    maintainType: r.maintainType,
    startDate: r.startDate,
    endDate: r.endDate,
    cost: r.cost,
    mechanic: r.mechanic
  }
  dialog.value = true
}

async function save() {
  const payload = { ...form.value, cost: Number(form.value.cost || 0) }
  if (editing.value) {
    await api.updateMaintain(editing.value, payload)
    ElMessage.success('维修记录已更新')
  } else {
    await api.createMaintain(payload)
    ElMessage.success('维修记录已建立')
  }
  dialog.value = false
  await load()
  emit('changed')
}

async function act(r, fn, okMsg) {
  try {
    await fn(r.id)
    ElMessage.success(okMsg)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="bar">
      <div>
        <h2>维修保养</h2>
        <div class="sub">当前农时：{{ season }}　·　报废农机不可再维修；开始维修把农机置「维修」，完成后回「待命」</div>
      </div>
      <el-button type="primary" @click="openCreate">新建维修单</el-button>
    </div>

    <div class="stat">
      共 {{ filtered.length }} 条记录 · 累计费用 {{ totalCost.toFixed(2) }} 元
      <span v-if="focusMachineId" class="zone">（已筛选：{{ machineLabel(focusMachineId) }}）</span>
    </div>

    <el-table :data="filtered" border stripe>
      <el-table-column label="农机" min-width="180">
        <template #default="{ row }">{{ machineLabel(row.machineId) }}</template>
      </el-table-column>
      <el-table-column prop="maintainType" label="维修类型" width="110" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column label="结束日期" width="120">
        <template #default="{ row }">{{ row.endDate || '—' }}</template>
      </el-table-column>
      <el-table-column label="费用(元)" width="110">
        <template #default="{ row }">{{ row.cost }}</template>
      </el-table-column>
      <el-table-column prop="mechanic" label="维修人" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === '已完成' ? 'success' : row.status === '维修中' ? 'primary' : 'warning'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status === '已完成'" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" :disabled="row.status !== '待修'" @click="act(row, api.startMaintain, '已开始维修，农机置维修')">开始维修</el-button>
          <el-button link type="success" :disabled="row.status !== '维修中'" @click="act(row, api.completeMaintain, '维修完成，农机回待命')">完成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑维修单' : '新建维修单'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="农机">
          <el-select v-model="form.machineId" placeholder="选择农机">
            <el-option
              v-for="m in machines"
              :key="m.id"
              :label="`${m.machineNo} ${m.machineType}（${m.status}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维修类型">
          <el-select v-model="form.maintainType">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="费用(元)">
          <el-input-number v-model="form.cost" :min="0" :step="100" :precision="2" />
        </el-form-item>
        <el-form-item label="维修人"><el-input v-model="form.mechanic" /></el-form-item>
        <el-form-item v-if="editing" label="状态">
          <el-select v-model="form.status">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.bar h2 { margin: 0 0 4px; font-size: 18px; }
.sub { font-size: 12px; color: var(--muted); }
.stat { margin-bottom: 10px; font-size: 13px; color: var(--muted); }
.zone { color: var(--primary); font-weight: 600; }
</style>
