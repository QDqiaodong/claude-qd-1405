<script setup>
import { computed, onMounted, ref } from 'vue'
import router from './router'
import api from './api'

import MachineView from './views/MachineView.vue'
import FieldView from './views/FieldView.vue'
import WorkOrderView from './views/WorkOrderView.vue'
import MaintainView from './views/MaintainView.vue'

// ------------------------------------------------------------------
// 导航形态：左侧农机 / 作业区列表 + 顶部农时下拉。
// 左侧上半区是四个业务模块入口；下半区是农机台账列表与作业区（按作物分组的地块）列表，
// 点击农机或作业区会把选中项传给当前视图做联动高亮；顶部「农时」下拉决定甘特时间窗。
// ------------------------------------------------------------------

const modules = [
  { path: '/machines', label: '农机台账', comp: MachineView },
  { path: '/fields', label: '地块台账', comp: FieldView },
  { path: '/work-orders', label: '作业订单', comp: WorkOrderView },
  { path: '/maintains', label: '维修保养', comp: MaintainView }
]

/** 农时下拉：决定甘特时间窗（相对今天的起止天数） */
const seasons = [
  { key: '春耕', desc: '整地播种期', from: 0, to: 14 },
  { key: '夏管', desc: '田间管理期', from: -7, to: 7 },
  { key: '秋收', desc: '收获期', from: -14, to: 0 },
  { key: '冬储', desc: '休整期', from: -30, to: 0 }
]

const currentPath = ref(router.currentRoute.value.path)
const season = ref(seasons[1].key)
const machines = ref([])
const fields = ref([])
const focusMachineId = ref(null)
const focusCrop = ref(null)

const currentView = computed(() => {
  const m = modules.find(i => i.path === currentPath.value)
  return m ? m.comp : MachineView
})

const seasonInfo = computed(() => seasons.find(s => s.key === season.value) || seasons[1])

/** 作业区：按作物类型把地块分组 */
const zones = computed(() => {
  const map = new Map()
  for (const f of fields.value) {
    const key = f.cropType || '未分类'
    if (!map.has(key)) {
      map.set(key, { crop: key, count: 0, area: 0 })
    }
    const z = map.get(key)
    z.count += 1
    z.area += Number(f.area || 0)
  }
  return [...map.values()]
})

function statusType(status) {
  if (status === '待命') return 'success'
  if (status === '作业中') return 'primary'
  if (status === '维修') return 'warning'
  return 'info'
}

async function loadSide() {
  try {
    const [m, f] = await Promise.all([api.listMachines(), api.listFields()])
    machines.value = m
    fields.value = f
  } catch (e) { /* 侧栏刷新失败不影响主流程 */ }
}

function selectMachine(id) {
  focusMachineId.value = focusMachineId.value === id ? null : id
}

function selectZone(crop) {
  focusCrop.value = focusCrop.value === crop ? null : crop
}

function onSelect(path) {
  if (path !== currentPath.value) {
    router.push(path)
  }
}

router.afterEach((to) => { currentPath.value = to.path })

onMounted(async () => {
  currentPath.value = router.currentRoute.value.path
  await loadSide()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">农</span>
        <div>
          <div class="brand-title">农机合作社</div>
          <div class="brand-sub">农机与作业调度管理 · 农机作业甘特</div>
        </div>
      </div>
      <div class="top-right">
        <span class="season-label">农时</span>
        <el-select v-model="season" class="season-select" size="large">
          <el-option
            v-for="s in seasons"
            :key="s.key"
            :label="`${s.key} · ${s.desc}`"
            :value="s.key"
          />
        </el-select>
        <span class="season-hint">{{ seasonInfo.desc }}</span>
      </div>
    </header>

    <div class="body">
      <aside class="sidebar">
        <el-menu class="nav-menu" :default-active="currentPath" @select="onSelect">
          <el-menu-item v-for="m in modules" :key="m.path" :index="m.path">
            {{ m.label }}
          </el-menu-item>
        </el-menu>

        <div class="side-block">
          <div class="side-title">农机（{{ machines.length }}）</div>
          <ul class="side-list">
            <li
              v-for="m in machines"
              :key="m.id"
              :class="['side-item', { active: focusMachineId === m.id }]"
              @click="selectMachine(m.id)"
            >
              <span class="item-main">{{ m.machineNo }} · {{ m.machineType }}</span>
              <el-tag size="small" :type="statusType(m.status)">{{ m.status }}</el-tag>
            </li>
          </ul>
        </div>

        <div class="side-block">
          <div class="side-title">作业区（{{ zones.length }}）</div>
          <ul class="side-list">
            <li
              v-for="z in zones"
              :key="z.crop"
              :class="['side-item', { active: focusCrop === z.crop }]"
              @click="selectZone(z.crop)"
            >
              <span class="item-main">{{ z.crop }}作业区</span>
              <span class="item-sub">{{ z.count }} 块 / {{ z.area.toFixed(0) }} 亩</span>
            </li>
          </ul>
        </div>

        <div class="hint">点击左侧农机或作业区可联动高亮；点击甘特条查看订单详情</div>
      </aside>

      <main class="content">
        <component
          :is="currentView"
          :season="season"
          :focus-machine-id="focusMachineId"
          :focus-crop="focusCrop"
          @changed="loadSide"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }

.topbar {
  height: 60px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  background: #fff; border-bottom: 1px solid var(--line);
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand-mark {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary); color: #fff; font-weight: 700; font-size: 18px;
}
.brand-title { font-size: 15px; font-weight: 600; }
.brand-sub { font-size: 12px; color: var(--muted); }
.top-right { display: flex; align-items: center; gap: 10px; }
.season-label { font-size: 13px; color: var(--muted); }
.season-select { width: 170px; }
.season-hint { font-size: 12px; color: var(--muted); }

.body { flex: 1; display: flex; min-height: 0; }

.sidebar {
  width: 240px; flex-shrink: 0;
  background: #fff; border-right: 1px solid var(--line);
  display: flex; flex-direction: column; overflow: auto;
}
.nav-menu { border-right: none; flex-shrink: 0; }
.side-block { border-top: 1px solid var(--line); padding: 8px 0; }
.side-title { padding: 6px 18px; font-size: 12px; color: var(--muted); font-weight: 600; }
.side-list { list-style: none; margin: 0; padding: 0; }
.side-item {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 7px 18px; cursor: pointer; font-size: 13px;
}
.side-item:hover { background: var(--primary-wash); }
.side-item.active { background: var(--el-color-primary-light-9); font-weight: 600; }
.item-main { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-sub { font-size: 12px; color: var(--muted); flex-shrink: 0; }
.hint { margin-top: auto; padding: 12px 18px; font-size: 12px; color: var(--muted); }

.content { flex: 1; padding: 20px 24px 28px; overflow: auto; }
</style>
