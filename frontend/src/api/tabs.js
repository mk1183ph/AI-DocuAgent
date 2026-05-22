import { http } from './http'

export async function fetchTabs() {
  const response = await http.get('/tabs')
  return response.data
}

export async function createTab(payload) {
  const formData = new FormData()
  formData.append('name', payload.name)
  formData.append('description', payload.description ?? '')
  formData.append('basePrompt', payload.basePrompt ?? '')
  formData.append('templateFile', payload.templateFile)

  const response = await http.post('/tabs', formData)
  return response.data
}

export async function deleteTab(tabId) {
  await http.delete(`/tabs/${tabId}`)
}
