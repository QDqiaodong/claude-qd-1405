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
const dialog = ref(false)
const editing = ref(null)
const keyword = ref('')

const types = ['拖拉机', '收割机', '播种机', '旋耕机', '植保机']
const statuses = ['待命', '作业中', '维修', '报废']

const emptyForm = () => ({
  machineNo: '',
  machineType: '拖拉机',
  brand: '',
  plateNo: '',
  workWidth: 2,
  purchaseDate: null,
  status: '待命'
})
const form = ref(emptyForm())

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return list.value.filter(m => {
    if (props.focusMachineId && m.id !== props.focusMachineId) return false
    if (!kw) return true
    return (m.machineNo || '').includes(kw) || (m.brand || '').includes(kw) || (m.plateNo || '').includes(kw)
  })
})

async function load() {
  list.value = await api.listMachines()
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    machineNo: r.machineNo,
    machineType: r.machineType,
    brand: r.brand,
    plateNo: r.plateNo,
    workWidth: r.workWidth,
    purchaseDate: r.purchaseDate,
    status: r.status
  }
  dialog.value = true
}

async function save() {
  const payload = { ...form.value, workWidth: Number(form.value.workWidth) }
  if (editing.value) {
    await api.updateMachine(editing.value, payload)
    ElMessage.success('农机已更新')
  } else {
    await api.createMachine(payload)
    ElMessage.success('农机已登记')
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

const isScrapped = (r) => r.status === '报废'

onMounted(load)
</script>

<template>
  <div>
    <div class="bar">
      <div>
        <h2>农机台账</h2>
        <div class="sub">当前农时：{{ season }}　·　只有「待命」农机可派工，报废为终态</div>
      </div>
      <div class="bar-right">
        <el-input v-model="keyword" placeholder="搜索机号 / 品牌 / 牌号" clearable class="search" />
        <el-button type="primary" @click="openCreate">登记农机</el-button>
      </div>
    </div>

    <el-table :data="filtered" border stripe>
      <el-table-column prop="machineNo" label="机号" width="110" />
      <el-table-column prop="machineType" label="类型" width="100" />
      <el-table-column prop="brand" label="品牌" width="120" />
      <el-table-column prop="plateNo" label="牌号" width="130" />
      <el-table-column label="作业幅宽(米)" width="120">
        <template #default="{ row }">{{ row.workWidth }}</template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="购入日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === '待命' ? 'success' : row.status === '作业中' ? 'primary' : row.status === '维修' ? 'warning' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="300">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="isScrapped(row)" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" :disabled="isScrapped(row)" @click="act(row, api.dispatchMachine, '已派工，农机置作业中')">派工</el-button>
          <el-button link type="success" :disabled="isScrapped(row)" @click="act(row, api.finishMachine, '已完工，农机回待命')">完工</el-button>
          <el-button link type="warning" :disabled="isScrapped(row)" @click="act(row, api.sendRepairMachine, '已送修，农机置维修')">送修</el-button>
          <el-button link type="success" :disabled="isScrapped(row)" @click="act(row, api.repairDoneMachine, '已修完，农机回待命')">修完</el-button>
          <el-button link type="danger" :disabled="isScrapped(row)" @click="act(row, api.scrapMachine, '农机已报废')">报废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑农机' : '登记农机'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="机号"><el-input v-model="form.machineNo" placeholder="如 NJ-009" /></el-form-item>
        <el-form-item label="农机类型">
          <el-select v-model="form.machineType">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="牌号"><el-input v-model="form.plateNo" /></el-form-item>
        <el-form-item label="作业幅宽(米)">
          <el-input-number v-model="form.workWidth" :min="0.1" :step="0.1" :precision="2" />
        </el-form-item>
        <el-form-item label="购入日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
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
.bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.bar h2 { margin: 0 0 4px; font-size: 18px; }
.sub { font-size: 12px; color: var(--muted); }
.bar-right { display: flex; gap: 10px; }
.search { width: 220px; }
</style>
