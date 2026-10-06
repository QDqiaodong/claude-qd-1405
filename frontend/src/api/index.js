import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

async function call(promise) {
  const response = await promise
  const body = response.data
  if (!body || body.ok === false) {
    throw new Error((body && body.message) || '请求失败')
  }
  return body.data
}

const params = (o) => ({ params: o })

export default {
  // ---------------- 农机 machine ----------------
  listMachines: () => call(http.get('/machines')),
  createMachine: (d) => call(http.post('/machines', d)),
  updateMachine: (id, d) => call(http.put(`/machines/${id}`, d)),
  dispatchMachine: (id) => call(http.post(`/machines/${id}/dispatch`)),
  finishMachine: (id) => call(http.post(`/machines/${id}/finish`)),
  sendRepairMachine: (id) => call(http.post(`/machines/${id}/send-repair`)),
  repairDoneMachine: (id) => call(http.post(`/machines/${id}/repair-done`)),
  scrapMachine: (id) => call(http.post(`/machines/${id}/scrap`)),

  // ---------------- 地块 field ----------------
  listFields: () => call(http.get('/fields')),
  createField: (d) => call(http.post('/fields', d)),
  updateField: (id, d) => call(http.put(`/fields/${id}`, d)),
  transferField: (id, status) => call(http.post(`/fields/${id}/status`, null, params({ status }))),

  // ---------------- 作业订单 workorder ----------------
  listWorkOrders: () => call(http.get('/work-orders')),
  ganttWorkOrders: (from, to) => call(http.get('/work-orders/gantt', params({ from, to }))),
  createWorkOrder: (d) => call(http.post('/work-orders', d)),
  updateWorkOrder: (id, d) => call(http.put(`/work-orders/${id}`, d)),
  startWorkOrder: (id) => call(http.post(`/work-orders/${id}/start`)),
  completeWorkOrder: (id) => call(http.post(`/work-orders/${id}/complete`)),
  cancelWorkOrder: (id) => call(http.post(`/work-orders/${id}/cancel`)),

  // ---------------- 维修保养 maintain ----------------
  listMaintains: () => call(http.get('/maintains')),
  createMaintain: (d) => call(http.post('/maintains', d)),
  updateMaintain: (id, d) => call(http.put(`/maintains/${id}`, d)),
  startMaintain: (id) => call(http.post(`/maintains/${id}/start`)),
  completeMaintain: (id) => call(http.post(`/maintains/${id}/complete`))
}
