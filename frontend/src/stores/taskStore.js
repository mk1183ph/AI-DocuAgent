import { defineStore } from 'pinia'
import {
  createTask,
  deleteTask,
  downloadDocx,
  fetchLatestDocument,
  fetchPlacementPreview,
  fetchReconstructionSummary,
  fetchTask,
  fetchTasks,
  fetchWritePlan,
  generateDraft,
  updateDocument,
  updateTask
} from '@/api/tasks'

function resolveErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message ?? fallbackMessage
}

function resolveGenerationErrorMessage(error) {
  const message = error?.response?.data?.message ?? ''
  const lowerMessage = message.toLowerCase()

  if (message.includes('Ollama')) {
    return 'Ollama가 실행 중인지 확인해주세요.'
  }
  if (message.includes('Gemini API 키') || message.includes('API 키')) {
    return message
  }
  if (message.includes('사용량 한도') || lowerMessage.includes('quota')) {
    return 'Gemini 사용량 한도를 초과했습니다.'
  }
  if (message.includes('모델') || lowerMessage.includes('model')) {
    return '설정된 모델을 찾을 수 없습니다.'
  }
  if (message.includes('JSON')) {
    return 'AI 응답을 JSON으로 해석하지 못했습니다.'
  }
  if (message.includes('시간') || lowerMessage.includes('timeout')) {
    return '요청 시간이 초과되었습니다.'
  }
  if (message.includes('Gemini API에 연결') || message.includes('네트워크') || message.includes('DNS') || message.includes('방화벽')) {
    return message
  }

  return message || 'AI 생성에 실패했습니다. 다시 시도해주세요.'
}

async function resolveDownloadErrorMessage(error) {
  const data = error?.response?.data
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text())
      return parsed?.message || '문서 다운로드 중 오류가 발생했습니다.'
    } catch (parseError) {
      return '문서 다운로드 중 오류가 발생했습니다.'
    }
  }

  return error?.response?.data?.message || '문서 생성에 실패했습니다.'
}

function isNotFound(error) {
  return error?.response?.status === 404
}

export const useTaskStore = defineStore('tasks', {
  state: () => ({
    tasks: [],
    selectedTaskId: null,
    selectedTask: null,
    generatedDraft: null,
    placementPreview: [],
    writePlan: [],
    reconstructionSummary: null,
    isLoading: false,
    isSaving: false,
    isGenerating: false,
    generationStepIndex: 0,
    generationDelayWarning: false,
    generationTimers: [],
    isLoadingDraft: false,
    isSavingDraft: false,
    isLoadingPlacementPreview: false,
    isLoadingWritePlan: false,
    isLoadingReconstructionSummary: false,
    isDownloadingDocx: false,
    errorMessage: ''
  }),
  getters: {
    selectedTaskSummary: (state) => state.tasks.find((task) => task.id === state.selectedTaskId) ?? null,
    hasSavedDraft: (state) => Boolean(state.generatedDraft)
  },
  actions: {
    async loadTasks(tabId) {
      if (!tabId) {
        this.reset()
        return
      }

      this.isLoading = true
      this.errorMessage = ''

      try {
        this.tasks = await fetchTasks(tabId)
        if (!this.tasks.some((task) => task.id === this.selectedTaskId)) {
          this.selectedTaskId = null
          this.selectedTask = null
          this.generatedDraft = null
          this.placementPreview = []
          this.writePlan = []
          this.reconstructionSummary = null
        }
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '작업 목록을 불러오지 못했습니다.')
      } finally {
        this.isLoading = false
      }
    },
    async addTask(tabId, payload) {
      this.isSaving = true
      this.errorMessage = ''

      try {
        const task = await createTask(tabId, payload)
        this.tasks = [task, ...this.tasks]
        this.selectedTaskId = task.id
        this.selectedTask = task
        this.generatedDraft = null
        this.placementPreview = []
        this.writePlan = []
        this.reconstructionSummary = null
        return task
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '작업을 저장하지 못했습니다.')
        throw error
      } finally {
        this.isSaving = false
      }
    },
    async selectTask(taskId) {
      this.selectedTaskId = taskId
      this.generatedDraft = null
      this.placementPreview = []
      this.writePlan = []
      this.reconstructionSummary = null
      this.errorMessage = ''

      try {
        this.selectedTask = await fetchTask(taskId)
        await this.loadLatestDraft(taskId)
        await this.loadReconstructionSummary(taskId)
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '작업 상세를 불러오지 못했습니다.')
      }
    },
    async loadLatestDraft(taskId) {
      this.isLoadingDraft = true
      this.errorMessage = ''

      try {
        this.generatedDraft = await fetchLatestDocument(taskId)
      } catch (error) {
        if (isNotFound(error)) {
          this.generatedDraft = null
          return null
        }
        this.errorMessage = resolveErrorMessage(error, '저장된 초안을 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoadingDraft = false
      }

      return this.generatedDraft
    },
    async saveTask(taskId, payload) {
      this.isSaving = true
      this.errorMessage = ''

      try {
        const task = await updateTask(taskId, payload)
        this.selectedTask = task
        this.tasks = this.tasks.map((item) => (item.id === task.id ? task : item))
        return task
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '작업을 수정하지 못했습니다.')
        throw error
      } finally {
        this.isSaving = false
      }
    },
    async generateDraftForTask(taskId, inferenceStrength) {
      this.isGenerating = true
      this.startGenerationProgress()
      this.errorMessage = ''

      try {
        this.generatedDraft = await generateDraft(taskId, { inferenceStrength })
        this.generationStepIndex = 3
        return this.generatedDraft
      } catch (error) {
        this.errorMessage = resolveGenerationErrorMessage(error)
        throw error
      } finally {
        this.stopGenerationProgress()
        this.isGenerating = false
      }
    },
    async saveDraft(documentId, generatedContent) {
      this.isSavingDraft = true
      this.errorMessage = ''

      try {
        this.generatedDraft = await updateDocument(documentId, { generatedContent })
        return this.generatedDraft
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '초안을 저장하지 못했습니다.')
        throw error
      } finally {
        this.isSavingDraft = false
      }
    },
    async loadPlacementPreview(taskId) {
      this.isLoadingPlacementPreview = true
      this.errorMessage = ''

      try {
        this.placementPreview = await fetchPlacementPreview(taskId)
        return this.placementPreview
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '문서 배치 미리보기를 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoadingPlacementPreview = false
      }
    },
    async loadWritePlan(taskId) {
      this.isLoadingWritePlan = true
      this.errorMessage = ''

      try {
        this.writePlan = await fetchWritePlan(taskId)
        return this.writePlan
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '문서 삽입 계획을 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoadingWritePlan = false
      }
    },
    async loadReconstructionSummary(taskId) {
      this.isLoadingReconstructionSummary = true
      this.errorMessage = ''

      try {
        this.reconstructionSummary = await fetchReconstructionSummary(taskId)
        return this.reconstructionSummary
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '문서 생성 결과를 불러오지 못했습니다.')
        throw error
      } finally {
        this.isLoadingReconstructionSummary = false
      }
    },
    async downloadCompletedDocx(taskId) {
      this.isDownloadingDocx = true
      this.errorMessage = ''

      try {
        const response = await downloadDocx(taskId)
        await this.loadReconstructionSummary(taskId)
        return response
      } catch (error) {
        this.errorMessage = await resolveDownloadErrorMessage(error)
        throw error
      } finally {
        this.isDownloadingDocx = false
      }
    },
    async removeTask(taskId) {
      this.errorMessage = ''

      try {
        await deleteTask(taskId)
        this.tasks = this.tasks.filter((task) => task.id !== taskId)
        if (this.selectedTaskId === taskId) {
          this.selectedTaskId = null
          this.selectedTask = null
          this.generatedDraft = null
          this.placementPreview = []
          this.writePlan = []
          this.reconstructionSummary = null
        }
      } catch (error) {
        this.errorMessage = resolveErrorMessage(error, '작업을 삭제하지 못했습니다.')
        throw error
      }
    },
    startGenerationProgress() {
      this.clearGenerationTimers()
      this.generationStepIndex = 0
      this.generationDelayWarning = false
      this.generationTimers = [
        window.setTimeout(() => {
          this.generationStepIndex = Math.max(this.generationStepIndex, 1)
        }, 1200),
        window.setTimeout(() => {
          this.generationStepIndex = Math.max(this.generationStepIndex, 2)
        }, 3200),
        window.setTimeout(() => {
          this.generationStepIndex = Math.max(this.generationStepIndex, 3)
        }, 5200),
        window.setTimeout(() => {
          this.generationDelayWarning = true
        }, 30000)
      ]
    },
    stopGenerationProgress() {
      this.clearGenerationTimers()
      this.generationDelayWarning = false
    },
    clearGenerationTimers() {
      this.generationTimers.forEach((timerId) => window.clearTimeout(timerId))
      this.generationTimers = []
    },
    reset() {
      this.clearGenerationTimers()
      this.tasks = []
      this.selectedTaskId = null
      this.selectedTask = null
      this.generatedDraft = null
      this.placementPreview = []
      this.writePlan = []
      this.reconstructionSummary = null
      this.isLoading = false
      this.isSaving = false
      this.isGenerating = false
      this.generationStepIndex = 0
      this.generationDelayWarning = false
      this.isLoadingDraft = false
      this.isSavingDraft = false
      this.isLoadingPlacementPreview = false
      this.isLoadingWritePlan = false
      this.isLoadingReconstructionSummary = false
      this.isDownloadingDocx = false
      this.errorMessage = ''
    }
  }
})
