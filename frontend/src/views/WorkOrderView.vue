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

/** 农时 → 甘特时间窗（相对今天的起止天数） */
const SEASON_WINDOW = {
  春耕: [0, 14],
  夏管: [-7, 7],
  秋收: [-14, 0],
  冬储: [-30, 0]
}

const workTypes = ['耕地', '播种', '施肥', '植保', '收割']

const orders = ref([])
const machines = ref([])
const fields = ref([])
const dialog = ref(false)
const detail = ref(false)
const editing = ref(null)
const current = ref(null)

const emptyForm = () => ({
  machineId: null,
  fieldId: null,
  operator: '',
  workType: '耕地',
  planDate: fmt(new Date()),
  workArea: 50,
  fuel: 0
})
const form = ref(emptyForm())

// ---------------- 日期工具 ----------------
function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function addDays(d, n) {
  const x = new Date(d.getTime())
  x.setDate(x.getDate() + n)
  return x
}

const window7 = computed(() => SEASON_WINDOW[props.season] || SEASON_WINDOW['夏管'])
const from = computed(() => fmt(addDays(new Date(), window7.value[0])))
const to = computed(() => fmt(addDays(new Date(), window7.value[1])))

const days = computed(() => {
  const out = []
  const [a, b] = window7.value
  const base = new Date()
  for (let i = a; i <= b; i++) {
    const d = addDays(base, i)
    out.push({ key: fmt(d), label: `${d.getMonth() + 1}/${d.getDate()}`, weekend: d.getDay() === 0 || d.getDay() === 6 })
  }
  return out
})

// ---------------- 甘特数据 ----------------
const visibleOrders = computed(() => {
  const crop = props.focusCrop
  return orders.value.filter(o => {
    if (o.planDate < from.value || o.planDate > to.value) return false
    if (props.focusMachineId && o.machineId !== props.focusMachineId) return false
    if (crop) {
      const f = fields.value.find(x => x.id === o.fieldId)
      if (!f || f.cropType !== crop) return false
    }
    return true
  })
})

const rows = computed(() => {
  const used = new Set(visibleOrders.value.map(o => o.machineId))
  return machines.value
    .filter(m => used.has(m.id) || !props.focusMachineId)
    .map(m => {
      const cells = days.value.map(d => ({
        key: d.key,
        items: visibleOrders.value.filter(o => o.machineId === m.id && o.planDate === d.key)
      }))
      return { machine: m, cells, count: cells.reduce((s, c) => s + c.items.length, 0) }
    })
    .filter(r => !props.focusMachineId || r.machine.id === props.focusMachineId || r.count > 0)
})

const gridCols = computed(() => `190px repeat(${days.value.length}, minmax(78px, 1fr))`)

const machineLabel = (id) => {
  const m = machines.value.find(x => x.id === id)
  return m ? `${m.machineNo} ${m.machineType}` : `#${id}`
}
const fieldLabel = (id) => {
  const f = fields.value.find(x => x.id === id)
  return f ? `${f.fieldName}（${f.area}亩）` : `#${id}`
}
const statusColor = (s) => ({
  待派工: 'st-plan',
  作业中: 'st-working',
  已完成: 'st-done',
  已取消: 'st-cancel'
}[s] || 'st-plan')

// ---------------- 交互 ----------------
async function load() {
  const [o, m, f] = await Promise.all([api.listWorkOrders(), api.listMachines(), api.listFields()])
  orders.value = o
  machines.value = m
  fields.value = f
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  dialog.value = true
}

function openEdit(o) {
  editing.value = o.id
  form.value = {
    machineId: o.machineId,
    fieldId: o.fieldId,
    operator: o.operator,
    workType: o.workType,
    planDate: o.planDate,
    workArea: o.workArea,
    fuel: o.fuel
  }
  dialog.value = true
}

function openDetail(o) {
  current.value = o
  detail.value = true
}

async function save() {
  const payload = {
    ...form.value,
    workArea: Number(form.value.workArea),
    fuel: Number(form.value.fuel || 0)
  }
  if (editing.value) {
    await api.updateWorkOrder(editing.value, payload)
    ElMessage.success('订单已更新')
  } else {
    await api.createWorkOrder(payload)
    ElMessage.success('订单已派工')
  }
  dialog.value = false
  await load()
  emit('changed')
}

async function act(o, fn, okMsg) {
  try {
    await fn(o.id)
    ElMessage.success(okMsg)
    detail.value = false
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
        <h2>作业订单 · 农机作业甘特</h2>
        <div class="sub">农时：{{ season }}（{{ from }} ~ {{ to }}）　·　行=农机，列=日期，条=作业订单，点击色条看详情</div>
      </div>
      <el-button type="primary" @click="openCreate">新建作业订单</el-button>
    </div>

    <div class="legend">
      <span v-for="(c, s) in { 待派工: 'st-plan', 作业中: 'st-working', 已完成: 'st-done', 已取消: 'st-cancel' }" :key="s">
        <i :class="['dot', c]"></i>{{ s }}
      </span>
    </div>

    <div class="gantt">
      <div class="gantt-head" :style="{ gridTemplateColumns: gridCols }">
        <div class="col-machine">农机</div>
        <div
          v-for="d in days"
          :key="d.key"
          :class="['col-day', { weekend: d.weekend }]"
        >{{ d.label }}</div>
      </div>

      <div v-for="r in rows" :key="r.machine.id" class="gantt-row" :style="{ gridTemplateColumns: gridCols }">
        <div class="col-machine">
          <span class="m-no">{{ r.machine.machineNo }}</span>
          <span class="m-type">{{ r.machine.machineType }}</span>
          <el-tag size="small" :type="r.machine.status === '待命' ? 'success' : r.machine.status === '作业中' ? 'primary' : r.machine.status === '维修' ? 'warning' : 'info'">
            {{ r.machine.status }}
          </el-tag>
        </div>
        <div
          v-for="c in r.cells"
          :key="c.key"
          :class="['col-day', { weekend: days.find(d => d.key === c.key)?.weekend }]"
        >
          <div
            v-for="o in c.items"
            :key="o.id"
            :class="['bar', statusColor(o.status)]"
            :title="`${o.workType} · ${fieldLabel(o.fieldId)} · ${o.workArea}亩`"
            @click="openDetail(o)"
          >
            {{ o.workType }} {{ o.workArea }}亩
          </div>
        </div>
      </div>

      <div v-if="rows.length === 0" class="empty">该农时窗口内暂无作业订单</div>
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialog" :title="editing ? '编辑作业订单' : '新建作业订单'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="农机">
          <el-select v-model="form.machineId" placeholder="选择待命农机">
            <el-option
              v-for="m in machines"
              :key="m.id"
              :label="`${m.machineNo} ${m.machineType}（${m.status}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="地块">
          <el-select v-model="form.fieldId" placeholder="选择地块">
            <el-option
              v-for="f in fields"
              :key="f.id"
              :label="`${f.fieldName} ${f.cropType}（${f.area}亩 · ${f.status}）`"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="农机手"><el-input v-model="form.operator" /></el-form-item>
        <el-form-item label="作业类型">
          <el-select v-model="form.workType">
            <el-option v-for="t in workTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="作业亩数">
          <el-input-number v-model="form.workArea" :min="0.1" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="油耗(升)">
          <el-input-number v-model="form.fuel" :min="0" :step="5" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 甘特条详情 -->
    <el-dialog v-model="detail" title="作业订单详情" width="460px">
      <div v-if="current">
        <div class="row"><span>农机</span><b>{{ machineLabel(current.machineId) }}</b></div>
        <div class="row"><span>地块</span><b>{{ fieldLabel(current.fieldId) }}</b></div>
        <div class="row"><span>农机手</span><b>{{ current.operator }}</b></div>
        <div class="row"><span>作业类型</span><b>{{ current.workType }}</b></div>
        <div class="row"><span>计划日期</span><b>{{ current.planDate }}</b></div>
        <div class="row"><span>作业亩数</span><b>{{ current.workArea }} 亩</b></div>
        <div class="row"><span>油耗</span><b>{{ current.fuel ?? 0 }} 升</b></div>
        <div class="row"><span>状态</span><b>{{ current.status }}</b></div>
      </div>
      <template #footer>
        <el-button @click="detail = false">关闭</el-button>
        <el-button
          v-if="current && current.status === '待派工'"
          type="primary"
          @click="act(current, api.startWorkOrder, '已开工，农机置作业中')"
        >开工</el-button>
        <el-button
          v-if="current && current.status === '作业中'"
          type="success"
          @click="act(current, api.completeWorkOrder, '已完工，农机回待命并推进地块状态')"
        >完工</el-button>
        <el-button
          v-if="current && (current.status === '待派工' || current.status === '作业中')"
          type="danger"
          @click="act(current, api.cancelWorkOrder, '订单已取消')"
        >取消</el-button>
        <el-button
          v-if="current && (current.status === '待派工' || current.status === '作业中')"
          @click="openEdit(current)"
        >编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.bar h2 { margin: 0 0 4px; font-size: 18px; }
.sub { font-size: 12px; color: var(--muted); }

.legend { display: flex; gap: 16px; font-size: 12px; color: var(--muted); margin-bottom: 8px; }
.legend span { display: flex; align-items: center; gap: 5px; }
.dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }

.gantt { background: #fff; border: 1px solid var(--line); border-radius: 8px; overflow: auto; }
.gantt-head, .gantt-row {
  display: grid;
  border-bottom: 1px solid var(--line);
}
.gantt-head { position: sticky; top: 0; background: var(--primary-wash); font-weight: 600; z-index: 2; }
.gantt-row { min-height: 46px; }
.gantt-row:hover { background: #FFFDFC; }
.col-machine {
  padding: 6px 10px; font-size: 13px; border-right: 1px solid var(--line);
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}
.m-no { font-weight: 600; }
.m-type { color: var(--muted); font-size: 12px; }
.col-day {
  padding: 4px; border-right: 1px solid #F1ECE9; font-size: 12px;
  display: flex; flex-direction: column; gap: 3px; justify-content: center;
}
.col-day.weekend { background: #FBF7F5; }
.gantt-head .col-day { padding: 8px 4px; text-align: center; justify-content: center; }

.bar {
  padding: 3px 6px; border-radius: 4px; color: #fff; font-size: 12px;
  cursor: pointer; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.bar:hover { filter: brightness(1.08); }

.st-plan { background: var(--primary-soft); }
.st-working { background: var(--primary); }
.st-done { background: #7CB342; }
.st-cancel { background: #B0A8A4; }
.dot.st-plan { background: var(--primary-soft); }
.dot.st-working { background: var(--primary); }
.dot.st-done { background: #7CB342; }
.dot.st-cancel { background: #B0A8A4; }

.empty { padding: 24px; text-align: center; color: var(--muted); }

.row { display: flex; justify-content: space-between; padding: 5px 0; color: var(--muted); }
.row b { color: var(--ink); font-weight: 600; }
</style>
