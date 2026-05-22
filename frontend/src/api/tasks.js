import { http } from './http'

export async function fetchTasks(tabId) {
  const response = await http.get(`/tabs/${tabId}/tasks`)
  return response.data
}

export async function createTask(tabId, payload) {
  const response = await http.post(`/tabs/${tabId}/tasks`, payload)
  return response.data
}

export async function fetchTask(taskId) {
  const response = await http.get(`/tasks/${taskId}`)
  return response.data
}

export async function updateTask(taskId, payload) {
  const response = await http.put(`/tasks/${taskId}`, payload)
  return response.data
}

export async function deleteTask(taskId) {
  await http.delete(`/tasks/${taskId}`)
}

export async function generateDraft(taskId, payload = {}) {
  const response = await http.post(`/tasks/${taskId}/generate-draft`, payload)
  return response.data
}

export async function fetchDocuments(taskId) {
  const response = await http.get(`/tasks/${taskId}/documents`)
  return response.data
}

export async function fetchLatestDocument(taskId) {
  const response = await http.get(`/tasks/${taskId}/documents/latest`)
  return response.data
}

export async function fetchDocument(documentId) {
  const response = await http.get(`/documents/${documentId}`)
  return response.data
}

export async function updateDocument(documentId, payload) {
  const response = await http.put(`/documents/${documentId}`, payload)
  return response.data
}

export async function fetchPlacementPreview(taskId) {
  const response = await http.get(`/tasks/${taskId}/placement-preview`)
  return response.data
}

export async function fetchWritePlan(taskId) {
  const response = await http.get(`/tasks/${taskId}/write-plan`)
  return response.data
}

export async function fetchReconstructionSummary(taskId) {
  const response = await http.get(`/tasks/${taskId}/reconstruction-summary`)
  return response.data
}

export async function downloadDocx(taskId) {
  const response = await http.get(`/tasks/${taskId}/download-docx`, {
    responseType: 'blob'
  })
  return {
    blob: response.data,
    headers: response.headers
  }
}
