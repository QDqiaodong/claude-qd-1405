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

const crops = ['小麦', '玉米', '水稻', '大豆', '油菜']
const soils = ['壤土', '砂土', '黏土']
const statuses = ['待耕', '已耕', '已播', '已收', '休耕']

const emptyForm = () => ({
  fieldNo: '',
  fieldName: '',
  area: 50,
  cropType: '小麦',
  soilType: '壤土',
  status: '待耕'
})
const form = ref(emptyForm())

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return list.value.filter(f => {
    if (props.focusCrop && f.cropType !== props.focusCrop) return false
    if (!kw) return true
    return (f.fieldNo || '').includes(kw) || (f.fieldName || '').includes(kw)
  })
})

const totalArea = computed(() => filtered.value.reduce((s, f) => s + Number(f.area || 0), 0))

async function load() {
  list.value = await api.listFields()
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    fieldNo: r.fieldNo,
    fieldName: r.fieldName,
    area: r.area,
    cropType: r.cropType,
    soilType: r.soilType,
    status: r.status
  }
  dialog.value = true
}

async function save() {
  const payload = { ...form.value, area: Number(form.value.area) }
  if (editing.value) {
    await api.updateField(editing.value, payload)
    ElMessage.success('地块已更新')
  } else {
    await api.createField(payload)
    ElMessage.success('地块已登记')
  }
  dialog.value = false
  await load()
  emit('changed')
}

async function advance(r, status) {
  try {
    await api.transferField(r.id, status)
    ElMessage.success(`地块已推进到「${status}」`)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

/** 下一档状态：待耕→已耕→已播→已收→待耕；休耕只能回到待耕 */
function nextStatus(status) {
  if (status === '待耕') return '已耕'
  if (status === '已耕') return '已播'
  if (status === '已播') return '已收'
  if (status === '已收') return '待耕'
  return '待耕'
}

onMounted(load)
</script>

<template>
  <div>
    <div class="bar">
      <div>
        <h2>地块台账</h2>
        <div class="sub">当前农时：{{ season }}　·　状态按 待耕→已耕→已播→已收 推进，不可跳级；休耕地块不可派作业</div>
      </div>
      <div class="bar-right">
        <el-input v-model="keyword" placeholder="搜索地块号 / 地块名" clearable class="search" />
        <el-button type="primary" @click="openCreate">登记地块</el-button>
      </div>
    </div>

    <div class="stat">
      共 {{ filtered.length }} 块地 · 合计 {{ totalArea.toFixed(2) }} 亩
      <span v-if="focusCrop" class="zone">（已筛选：{{ focusCrop }}作业区）</span>
    </div>

    <el-table :data="filtered" border stripe>
      <el-table-column prop="fieldNo" label="地块号" width="120" />
      <el-table-column prop="fieldName" label="地块名" width="140" />
      <el-table-column label="面积(亩)" width="110">
        <template #default="{ row }">{{ row.area }}</template>
      </el-table-column>
      <el-table-column prop="cropType" label="作物" width="100" />
      <el-table-column prop="soilType" label="土壤" width="100" />
      <el-table-column label="耕作状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === '休耕' ? 'info' : row.status === '已收' ? 'success' : 'primary'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="advance(row, nextStatus(row.status))">
            推进到「{{ nextStatus(row.status) }}」
          </el-button>
          <el-button v-if="row.status !== '休耕'" link type="warning" @click="advance(row, '休耕')">置休耕</el-button>
          <el-button v-else link type="warning" @click="advance(row, '待耕')">恢复待耕</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑地块' : '登记地块'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="地块号"><el-input v-model="form.fieldNo" placeholder="如 DK-A03" /></el-form-item>
        <el-form-item label="地块名"><el-input v-model="form.fieldName" /></el-form-item>
        <el-form-item label="面积(亩)">
          <el-input-number v-model="form.area" :min="0.1" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="作物类型">
          <el-select v-model="form.cropType">
            <el-option v-for="c in crops" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="土壤类型">
          <el-select v-model="form.soilType">
            <el-option v-for="s in soils" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editing" label="耕作状态">
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
.bar-right { display: flex; gap: 10px; }
.search { width: 220px; }
.stat { margin-bottom: 10px; font-size: 13px; color: var(--muted); }
.zone { color: var(--primary); font-weight: 600; }
</style>
