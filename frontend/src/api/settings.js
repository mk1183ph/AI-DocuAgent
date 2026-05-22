import { http } from './http'

export async function fetchSettings() {
  const response = await http.get('/settings')
  return response.data
}

export async function updateSettings(payload) {
  const response = await http.put('/settings', payload)
  return response.data
}
