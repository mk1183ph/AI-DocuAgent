import { http } from './http'

export async function analyzeTemplate(tabId) {
  const response = await http.post(`/tabs/${tabId}/analyze-template`)
  return response.data
}

export async function fetchTemplateAnalysis(tabId) {
  const response = await http.get(`/tabs/${tabId}/template-analysis`)
  return response.data
}

export async function fetchTemplateMappings(tabId) {
  const response = await http.get(`/tabs/${tabId}/template-mappings`)
  return response.data
}

export async function updateTemplateMappings(tabId, payload) {
  const response = await http.put(`/tabs/${tabId}/template-mappings`, payload)
  return response.data
}

export async function improveTemplateFieldRecommendations(tabId) {
  const response = await http.post(`/tabs/${tabId}/template-fields/recommendations`)
  return response.data
}
