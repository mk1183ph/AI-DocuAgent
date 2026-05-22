import { defineStore } from 'pinia'
import {
  analyzeTemplate,
  fetchTemplateAnalysis,
  fetchTemplateMappings,
  improveTemplateFieldRecommendations,
  updateTemplateMappings
} from '@/api/templateAnalysis'

function resolveErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message ?? fallbackMessage
}

function isNotFound(error) {
  return error?.response?.status === 404
}

export const useTemplateAnalysisStore = defineStore('templateAnalysis', {
  state: () => ({
    analysis: null,
    mappings: [],
    isLoading: false,
    isSavingMappings: false,
    isRecommendingFields: false,
    errorMessage: ''
  }),
  getters: {
    hasAnalysis: (state) => Boolean(state.analysis),
    labels: (state) => state.analysis?.labels ?? [],
    blocks: (state) => state.analysis?.blocks ?? [],
    hasMappings: (state) => state.mappings.length > 0
  },
  actions: {
    async analyze(tabId) {
      this.isLoading = true
      this.errorMessage = ''

      try {
        this.analysis = await analyzeTemplate(tabId)
        this.mappings = this.analysis.mappings ?? []
        return this.analysis
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, 'DOCX 양식 구조를 분석하지 못했습니다.')
        throw error
      } finally {
        this.isLoading = false
      }
    },
    async loadAnalysis(tabId) {
      if (!tabId) {
        this.reset()
        return null
      }

      this.isLoading = true
      this.errorMessage = ''

      try {
        this.analysis = await fetchTemplateAnalysis(tabId)
        this.mappings = this.analysis.mappings ?? []
        return this.analysis
      } catch (error) {
        if (isNotFound(error)) {
          this.analysis = null
          this.mappings = []
          return null
        }
        this.errorMessage = resolveErrorMessage(error, '저장된 양식 분석 결과를 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoading = false
      }
    },
    async loadMappings(tabId) {
      this.errorMessage = ''

      try {
        this.mappings = await fetchTemplateMappings(tabId)
        return this.mappings
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '문서 필드 매핑을 불러오지 못했습니다.')
        throw error
      }
    },
    async saveMappings(tabId, payload) {
      this.isSavingMappings = true
      this.errorMessage = ''

      try {
        this.mappings = await updateTemplateMappings(tabId, payload)
        if (this.analysis) {
          this.analysis = {
            ...this.analysis,
            mappings: this.mappings
          }
        }
        return this.mappings
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '문서 필드 매핑을 저장하지 못했습니다.')
        throw error
      } finally {
        this.isSavingMappings = false
      }
    },
    async improveRecommendations(tabId) {
      this.isRecommendingFields = true
      this.errorMessage = ''

      try {
        this.mappings = await improveTemplateFieldRecommendations(tabId)
        if (this.analysis) {
          this.analysis = {
            ...this.analysis,
            mappings: this.mappings
          }
        }
        return this.mappings
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, 'AI 필드 추천을 개선하지 못했습니다.')
        throw error
      } finally {
        this.isRecommendingFields = false
      }
    },
    reset() {
      this.analysis = null
      this.mappings = []
      this.isLoading = false
      this.isSavingMappings = false
      this.isRecommendingFields = false
      this.errorMessage = ''
    }
  }
})
