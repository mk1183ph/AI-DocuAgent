import { defineStore } from 'pinia'
import { createTab, deleteTab, fetchTabs } from '@/api/tabs'

function resolveErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message ?? fallbackMessage
}

export const useTabStore = defineStore('tabs', {
  state: () => ({
    tabs: [],
    selectedTabId: null,
    isLoading: false,
    isDeleting: false,
    errorMessage: ''
  }),
  getters: {
    selectedTab: (state) => state.tabs.find((tab) => tab.id === state.selectedTabId) ?? null
  },
  actions: {
    async loadTabs() {
      this.isLoading = true
      this.errorMessage = ''

      try {
        this.tabs = await fetchTabs()
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '탭 목록을 불러오지 못했습니다.')
      } finally {
        this.isLoading = false
      }
    },
    async addTab(payload) {
      this.errorMessage = ''

      try {
        const tab = await createTab(payload)
        this.tabs = [tab, ...this.tabs]
        this.selectedTabId = tab.id
        return tab
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '탭을 저장하지 못했습니다.')
        throw error
      }
    },
    selectTab(tabId) {
      this.selectedTabId = tabId
    },
    async removeTab(tabId) {
      this.isDeleting = true
      this.errorMessage = ''

      try {
        await deleteTab(tabId)
        this.tabs = this.tabs.filter((tab) => tab.id !== tabId)
        if (this.selectedTabId === tabId) {
          this.selectedTabId = this.tabs[0]?.id ?? null
        }
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '탭을 삭제하지 못했습니다.')
        throw error
      } finally {
        this.isDeleting = false
      }
    }
  }
})
