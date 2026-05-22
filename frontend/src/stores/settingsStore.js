import { defineStore } from 'pinia'
import { fetchSettings, updateSettings } from '@/api/settings'

function resolveErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message ?? fallbackMessage
}

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    settings: null,
    isLoading: false,
    isSaving: false,
    errorMessage: ''
  }),
  actions: {
    async loadSettings() {
      this.isLoading = true
      this.errorMessage = ''

      try {
        this.settings = await fetchSettings()
        return this.settings
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, 'AI 설정을 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoading = false
      }
    },
    async saveSettings(payload) {
      this.isSaving = true
      this.errorMessage = ''

      try {
        this.settings = await updateSettings(payload)
        return this.settings
      } catch (error) {
        this.errorMessage = 'AI 설정을 저장하지 못했습니다. 잠시 후 다시 시도해주세요.'
        throw error
      } finally {
        this.isSaving = false
      }
    }
  }
})
