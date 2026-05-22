<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import TabCreateModal from '@/components/TabCreateModal.vue'
import TaskCreateModal from '@/components/TaskCreateModal.vue'
import { useTabStore } from '@/stores/tabStore'
import { useTaskStore } from '@/stores/taskStore'
import { useTemplateAnalysisStore } from '@/stores/templateAnalysisStore'

const tabStore = useTabStore()
const taskStore = useTaskStore()
const templateAnalysisStore = useTemplateAnalysisStore()

const isCreateModalOpen = ref(false)
const isTaskModalOpen = ref(false)
const isCreatingTab = ref(false)
const tabModalErrorMessage = ref('')
const taskModalErrorMessage = ref('')
const saveMessage = ref('')
const draftSaveMessage = ref('')
const analysisMessage = ref('')
const mappingMessage = ref('')
const placementMessage = ref('')
const writePlanMessage = ref('')
const tabDeleteMessage = ref('')
const tabDeleteCandidate = ref(null)
const successToastMessage = ref('')
const inferenceStrength = ref(55)
const processingTab = ref('summary')
const draftFields = reactive({})

const fieldEditModalOpen = ref(false)
const fieldEditMode = ref('edit')
const fieldEditIndex = ref(-1)
const fieldAdvancedOpen = ref(false)
const fieldEditForm = reactive({
  sourceLabel: '',
  fieldKey: '',
  displayName: '',
  description: '',
  required: false,
  writingRule: ''
})

const generationProgressSteps = [
  '입력 내용 분석 중',
  'AI 추론 실행 중',
  '응답 검증 중',
  '초안 구성 중'
]

const selectedTab = computed(() => tabStore.selectedTab)
const selectedTask = computed(() => taskStore.selectedTask)
const generatedDraft = computed(() => taskStore.generatedDraft)
const hasTasks = computed(() => taskStore.tasks.length > 0)
const analysis = computed(() => templateAnalysisStore.analysis)
const detectedLabels = computed(() => templateAnalysisStore.labels)
const previewBlocks = computed(() => templateAnalysisStore.blocks.slice(0, 14))
const fieldMappings = computed(() => templateAnalysisStore.mappings)
const placementPreview = computed(() => taskStore.placementPreview)
const writePlan = computed(() => taskStore.writePlan)
const reconstructionSummary = computed(() => taskStore.reconstructionSummary)
const isInteractionLocked = computed(() => taskStore.isGenerating)
const activeGenerationStep = computed(() => generationProgressSteps[taskStore.generationStepIndex] ?? generationProgressSteps[0])
const skippedReconstructionResults = computed(() =>
  (reconstructionSummary.value?.results ?? []).filter((result) => result.status !== 'WRITTEN')
)
const draftFieldEntries = computed(() =>
  Object.entries(draftFields).map(([key, value]) => ({
    key,
    value,
    label: draftFieldLabel(key)
  }))
)

onMounted(() => {
  tabStore.loadTabs()
})

watch(
  () => tabStore.selectedTabId,
  (tabId) => {
    taskStore.loadTasks(tabId)
    templateAnalysisStore.reset()
    if (tabId) {
      templateAnalysisStore.loadAnalysis(tabId).catch(() => {})
    }
    saveMessage.value = ''
    draftSaveMessage.value = ''
    analysisMessage.value = ''
    mappingMessage.value = ''
    placementMessage.value = ''
    writePlanMessage.value = ''
    processingTab.value = 'summary'
    resetDraftFields()
  }
)

watch(
  selectedTask,
  (task) => {
    taskForm.title = task?.title ?? ''
    taskForm.userContext = task?.userContext ?? ''
    saveMessage.value = ''
    draftSaveMessage.value = ''
    placementMessage.value = ''
    writePlanMessage.value = ''
    processingTab.value = 'summary'
    resetDraftFields()
  }
)

watch(
  generatedDraft,
  (draft) => {
    syncDraftFields(draft)
    draftSaveMessage.value = ''
  }
)

const taskForm = reactive({
  title: '',
  userContext: ''
})

async function createTab(payload) {
  if (isInteractionLocked.value) return

  tabModalErrorMessage.value = ''
  isCreatingTab.value = true
  try {
    await tabStore.addTab(payload)
    isCreateModalOpen.value = false
  } catch (error) {
    tabModalErrorMessage.value = tabStore.errorMessage || '탭을 저장하지 못했습니다.'
  } finally {
    isCreatingTab.value = false
  }
}

function selectTab(tabId) {
  if (isInteractionLocked.value) return
  tabStore.selectTab(tabId)
}

function requestDeleteTab(tab) {
  if (isInteractionLocked.value) return
  tabDeleteCandidate.value = tab
  tabDeleteMessage.value = ''
}

function cancelDeleteTab() {
  tabDeleteCandidate.value = null
  tabDeleteMessage.value = ''
}

async function confirmDeleteTab() {
  if (!tabDeleteCandidate.value || isInteractionLocked.value) return

  const tabId = tabDeleteCandidate.value.id
  tabDeleteMessage.value = ''
  try {
    await tabStore.removeTab(tabId)
    if (tabStore.selectedTabId === null) {
      taskStore.reset()
      templateAnalysisStore.reset()
    }
    tabDeleteCandidate.value = null
    saveMessage.value = '탭을 삭제했습니다.'
    showSuccessToast('탭을 삭제했습니다.')
  } catch (error) {
    tabDeleteMessage.value = tabStore.errorMessage || '탭을 삭제하지 못했습니다.'
  }
}

async function analyzeTemplate() {
  if (!selectedTab.value || isInteractionLocked.value) return

  analysisMessage.value = ''
  mappingMessage.value = ''
  try {
    await templateAnalysisStore.analyze(selectedTab.value.id)
    analysisMessage.value = '문서 구조를 분석해 필드를 자동 추론했습니다. 필요하면 수정하세요.'
  } catch (error) {
    analysisMessage.value = templateAnalysisStore.errorMessage || 'DOCX 양식 구조를 분석하지 못했습니다.'
  }
}

async function recommendFields() {
  if (!selectedTab.value || isInteractionLocked.value) return

  mappingMessage.value = ''
  try {
    await templateAnalysisStore.improveRecommendations(selectedTab.value.id)
    mappingMessage.value = 'AI 필드 추천을 개선했습니다. 수정된 내용을 확인해주세요.'
  } catch (error) {
    mappingMessage.value = templateAnalysisStore.errorMessage || 'AI 필드 추천을 개선하지 못했습니다.'
  }
}

async function confirmField(mapping) {
  await saveFieldMappings(
    fieldMappings.value.map((item) =>
      sameField(item, mapping)
        ? { ...item, mappingStatus: 'CONFIRMED' }
        : item
    ),
    '필드를 확인 완료로 표시했습니다.'
  )
}

function openEditField(mapping, index) {
  fieldEditMode.value = 'edit'
  fieldEditIndex.value = index
  fieldAdvancedOpen.value = false
  setFieldEditForm(mapping)
  fieldEditModalOpen.value = true
}

function openCustomField() {
  fieldEditMode.value = 'custom'
  fieldEditIndex.value = -1
  fieldAdvancedOpen.value = false
  setFieldEditForm({
    sourceLabel: '',
    fieldKey: '',
    displayName: '',
    description: '',
    required: false,
    writingRule: ''
  })
  fieldEditModalOpen.value = true
}

function closeFieldEditModal() {
  fieldEditModalOpen.value = false
}

async function saveFieldEdit() {
  if (isInteractionLocked.value || !fieldEditForm.displayName.trim()) return

  const editedField = {
    sourceLabel: fieldEditForm.sourceLabel.trim(),
    fieldKey: fieldEditForm.fieldKey.trim(),
    displayName: fieldEditForm.displayName.trim(),
    description: fieldEditForm.description.trim(),
    required: fieldEditForm.required,
    writingRule: fieldEditForm.writingRule.trim(),
    mappingStatus: fieldEditMode.value === 'custom' ? 'CUSTOM' : 'EDITED',
    confidenceLevel: fieldEditMode.value === 'custom' ? 'LOW' : 'MEDIUM'
  }

  const nextMappings = [...fieldMappings.value]
  if (fieldEditMode.value === 'custom') {
    nextMappings.push(editedField)
  } else if (fieldEditIndex.value >= 0) {
    nextMappings.splice(fieldEditIndex.value, 1, {
      ...nextMappings[fieldEditIndex.value],
      ...editedField
    })
  }

  await saveFieldMappings(nextMappings, '문서 필드를 저장했습니다.')
  closeFieldEditModal()
}

async function saveFieldMappings(mappings, successMessage) {
  if (!selectedTab.value || isInteractionLocked.value) return

  mappingMessage.value = ''
  try {
    await templateAnalysisStore.saveMappings(
      selectedTab.value.id,
      mappings.map(toMappingPayload)
    )
    mappingMessage.value = successMessage
  } catch (error) {
    mappingMessage.value = templateAnalysisStore.errorMessage || '문서 필드를 저장하지 못했습니다.'
    throw error
  }
}

function toMappingPayload(mapping) {
  return {
    id: mapping.id ?? null,
    sourceLabel: mapping.sourceLabel || mapping.displayName || '사용자 필드',
    fieldKey: mapping.fieldKey ?? mapping.semanticFieldKey ?? null,
    semanticFieldKey: mapping.fieldKey ?? mapping.semanticFieldKey ?? null,
    displayName: mapping.displayName || mapping.sourceLabel || '사용자 필드',
    description: mapping.description || '',
    required: Boolean(mapping.required),
    mappingStatus: mapping.mappingStatus || 'EDITED',
    confidenceLevel: mapping.confidenceLevel || 'MEDIUM',
    writingRule: mapping.writingRule || ''
  }
}

function setFieldEditForm(mapping) {
  fieldEditForm.sourceLabel = mapping.sourceLabel ?? ''
  fieldEditForm.fieldKey = mapping.fieldKey ?? mapping.semanticFieldKey ?? ''
  fieldEditForm.displayName = mapping.displayName ?? mapping.sourceLabel ?? ''
  fieldEditForm.description = mapping.description ?? ''
  fieldEditForm.required = Boolean(mapping.required)
  fieldEditForm.writingRule = mapping.writingRule ?? ''
}

function sameField(left, right) {
  return (left.fieldKey ?? left.semanticFieldKey ?? left.sourceLabel) === (right.fieldKey ?? right.semanticFieldKey ?? right.sourceLabel)
}

async function createTask(payload) {
  if (!selectedTab.value || isInteractionLocked.value) return

  taskModalErrorMessage.value = ''
  try {
    await taskStore.addTask(selectedTab.value.id, payload)
    isTaskModalOpen.value = false
  } catch (error) {
    taskModalErrorMessage.value = taskStore.errorMessage || '작업을 저장하지 못했습니다.'
  }
}

async function selectTask(taskId) {
  if (isInteractionLocked.value) return
  await taskStore.selectTask(taskId)
}

async function saveTask() {
  if (!selectedTask.value || isInteractionLocked.value) return

  saveMessage.value = ''
  try {
    await taskStore.saveTask(selectedTask.value.id, {
      title: taskForm.title.trim(),
      userContext: taskForm.userContext.trim()
    })
    saveMessage.value = '변경사항을 저장했습니다.'
  } catch (error) {
    saveMessage.value = taskStore.errorMessage || '작업을 수정하지 못했습니다.'
  }
}

async function generateDraft() {
  if (!selectedTask.value || isInteractionLocked.value) return

  saveMessage.value = ''
  draftSaveMessage.value = ''
  try {
    await taskStore.generateDraftForTask(selectedTask.value.id, inferenceStrength.value)
    saveMessage.value = 'AI 초안을 생성했습니다.'
    processingTab.value = 'summary'
  } catch (error) {
    saveMessage.value = taskStore.errorMessage || 'AI 생성에 실패했습니다. 다시 시도해주세요.'
  }
}

async function saveDraft() {
  if (isInteractionLocked.value) return
  if (!generatedDraft.value) {
    draftSaveMessage.value = '저장할 초안이 없습니다.'
    return
  }

  draftSaveMessage.value = ''
  try {
    await taskStore.saveDraft(generatedDraft.value.id, JSON.stringify(normalizedDraftFields()))
    draftSaveMessage.value = '초안을 저장했습니다.'
  } catch (error) {
    draftSaveMessage.value = taskStore.errorMessage || '초안을 저장하지 못했습니다.'
  }
}

async function loadPlacementPreview() {
  if (!selectedTask.value || isInteractionLocked.value) return

  placementMessage.value = ''
  try {
    await taskStore.loadPlacementPreview(selectedTask.value.id)
    placementMessage.value = '문서 배치 미리보기를 불러왔습니다.'
    processingTab.value = 'placement'
  } catch (error) {
    placementMessage.value = taskStore.errorMessage || '문서 배치 미리보기를 불러오지 못했습니다.'
  }
}

async function loadWritePlan() {
  if (!selectedTask.value || isInteractionLocked.value) return

  writePlanMessage.value = ''
  try {
    await taskStore.loadWritePlan(selectedTask.value.id)
    writePlanMessage.value = '문서 삽입 계획을 불러왔습니다.'
    processingTab.value = 'plan'
  } catch (error) {
    writePlanMessage.value = taskStore.errorMessage || '문서 삽입 계획을 불러오지 못했습니다.'
  }
}

async function downloadCompletedDocx() {
  if (!selectedTask.value || !generatedDraft.value || isInteractionLocked.value) {
    draftSaveMessage.value = '다운로드할 저장된 초안이 없습니다.'
    return
  }

  draftSaveMessage.value = ''
  try {
    const response = await taskStore.downloadCompletedDocx(selectedTask.value.id)
    triggerFileDownload(response.blob, downloadFileName(response.headers, selectedTask.value.title))
    draftSaveMessage.value = '완성 문서를 다운로드했습니다.'
    processingTab.value = 'summary'
    showSuccessToast('완성 문서를 다운로드했습니다.')
  } catch (error) {
    draftSaveMessage.value = taskStore.errorMessage || '문서 다운로드 중 오류가 발생했습니다.'
  }
}

async function deleteSelectedTask() {
  if (!selectedTask.value || isInteractionLocked.value) return

  const taskId = selectedTask.value.id
  await taskStore.removeTask(taskId)
  saveMessage.value = '작업을 삭제했습니다.'
  draftSaveMessage.value = ''
}

function syncDraftFields(draft) {
  resetDraftFields()
  if (!draft) return

  const structuredContent = draft.structuredContent ?? parseStructuredContent(draft.generatedContent)
  Object.entries(structuredContent).forEach(([key, value]) => {
    draftFields[key] = value ?? ''
  })
}

function resetDraftFields() {
  Object.keys(draftFields).forEach((key) => {
    delete draftFields[key]
  })
}

function parseStructuredContent(generatedContent) {
  if (!generatedContent) return {}

  try {
    const parsed = JSON.parse(generatedContent)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : { content: generatedContent }
  } catch (error) {
    return { content: generatedContent }
  }
}

function normalizedDraftFields() {
  return Object.fromEntries(
    Object.entries(draftFields).map(([key, value]) => [key, String(value ?? '').trim()])
  )
}

function draftFieldLabel(key) {
  const mapping = fieldMappings.value.find((item) => (item.fieldKey ?? item.semanticFieldKey) === key)
  return mapping?.displayName || key
}

function formatDate(value) {
  if (!value) return ''

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function previewText(value) {
  if (!value) return ''
  return value.length > 90 ? `${value.slice(0, 90)}...` : value
}

function blockTypeLabel(type) {
  return type === 'TABLE_CELL' ? '표 셀' : '문단'
}

function blockLocation(block) {
  if (block.type !== 'TABLE_CELL') return `순서 ${block.order}`
  return `표 ${(block.tableIndex ?? 0) + 1} · 행 ${(block.rowIndex ?? 0) + 1} · 셀 ${(block.cellIndex ?? 0) + 1}`
}

function confidenceLabel(value) {
  if (value === 'HIGH') return '높음'
  if (value === 'MEDIUM') return '확인 권장'
  return '검토 필요'
}

function confidenceClass(value) {
  if (value === 'HIGH') return 'bg-emerald-50 text-emerald-700'
  if (value === 'MEDIUM') return 'bg-amber-50 text-amber-700'
  return 'bg-red-50 text-red-700'
}

function statusLabel(value) {
  if (value === 'CONFIRMED') return '확인 완료'
  if (value === 'EDITED') return '수정됨'
  if (value === 'CUSTOM') return '직접 추가'
  return '자동 추천'
}

function placementStatusLabel(status) {
  if (status === 'READY') return '준비됨'
  if (status === 'NO_DRAFT_VALUE') return '초안 내용 없음'
  if (status === 'NO_TARGET_BLOCK') return '대상 위치 없음'
  if (status === 'NEEDS_MANUAL_REVIEW') return '확인 필요'
  if (status === 'WRITTEN') return '작성 완료'
  if (status === 'SKIPPED_NO_VALUE') return '내용 없음'
  if (status === 'SKIPPED_NO_TARGET') return '대상 위치 없음'
  if (status === 'SKIPPED_WRITE_FAILED') return '작성 실패'
  return status
}

function placementStatusClass(status) {
  if (status === 'READY' || status === 'WRITTEN') return 'bg-emerald-50 text-emerald-700'
  if (status === 'NO_DRAFT_VALUE' || status === 'NEEDS_MANUAL_REVIEW' || status === 'SKIPPED_NO_VALUE') return 'bg-amber-50 text-amber-700'
  return 'bg-red-50 text-red-700'
}

function reconstructionStatusLabel(status) {
  return placementStatusLabel(status)
}

function reconstructionStatusClass(status) {
  return placementStatusClass(status)
}

function operationTypeLabel(type) {
  if (type === 'WRITE_TO_ADJACENT_CELL') return '오른쪽 셀에 작성'
  if (type === 'WRITE_TO_NEXT_CELL') return '같은 행의 다음 셀에 작성'
  if (type === 'INSERT_AFTER_PARAGRAPH') return '문단 아래에 삽입'
  return type || '확인 필요'
}

function writePlanLocation(operation) {
  if (operation.targetBlockType === 'PARAGRAPH') return `문단 #${operation.targetBlockOrder}`
  if (operation.tableIndex === null || operation.tableIndex === undefined) return '대상 위치 없음'
  return `표 ${(operation.tableIndex ?? 0) + 1} · 행 ${(operation.rowIndex ?? 0) + 1} · 셀 ${(operation.cellIndex ?? 0) + 1}`
}

function isErrorMessage(message) {
  return /실패|오류|못|없습니다|확인/.test(message)
}

function showSuccessToast(message) {
  successToastMessage.value = message
  window.setTimeout(() => {
    if (successToastMessage.value === message) {
      successToastMessage.value = ''
    }
  }, 3200)
}

function downloadFileName(headers, fallbackTitle) {
  const disposition = headers?.['content-disposition'] ?? headers?.['Content-Disposition']
  const match = disposition?.match(/filename\*=UTF-8''([^;]+)|filename="?([^"]+)"?/)
  if (match?.[1]) return decodeURIComponent(match[1])
  if (match?.[2]) return match[2]
  return `${(fallbackTitle || 'document').replace(/[\\/:*?"<>|]/g, '_')}.docx`
}

function triggerFileDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-slate-50 text-slate-900">
    <Sidebar
      :tabs="tabStore.tabs"
      :selected-tab-id="tabStore.selectedTabId"
      :is-loading="tabStore.isLoading"
      :is-locked="isInteractionLocked"
      @create-tab="isCreateModalOpen = true"
      @select-tab="selectTab"
      @delete-tab="requestDeleteTab"
    />

    <main class="flex-1 overflow-y-auto">
      <div class="mx-auto max-w-[1600px] px-8 py-8">
        <section v-if="!selectedTab" class="relative flex min-h-[calc(100vh-4rem)] items-center justify-center overflow-hidden rounded-[2rem] border border-white/80 bg-white px-8 py-12 text-center shadow-[0_24px_80px_rgba(15,23,42,0.08)]">
          <div class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-400" aria-hidden="true" />
          <div class="relative max-w-3xl">
            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600 via-violet-600 to-sky-500 text-sm font-black tracking-tight text-white shadow-xl shadow-indigo-500/20">
              AI
            </div>
            <p class="mt-8 text-xs font-bold uppercase tracking-[0.22em] text-violet-600">
              AI-POWERED DOCUMENT WORKSPACE
            </p>
            <h2 class="mt-4 text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
              템플릿 기반 문서 자동화
            </h2>
            <p class="mx-auto mt-5 max-w-2xl text-base leading-7 text-slate-600">
              기존 DOCX 양식을 그대로 업로드하면 AI가 구조를 이해하고 문서 초안을 완성합니다.
            </p>
            <button
              type="button"
              class="mt-8 inline-flex items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-slate-900/10 transition hover:-translate-y-0.5 hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-violet-300 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
              :disabled="isInteractionLocked"
              @click="isCreateModalOpen = true"
            >
              <span class="text-base leading-none">+</span>
              탭 추가
            </button>
            <div class="mx-auto mt-10 grid max-w-2xl gap-3 text-left sm:grid-cols-3">
              <div class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 shadow-sm">
                <p class="text-xs font-semibold text-slate-500">01</p>
                <p class="mt-2 text-sm font-semibold text-slate-900">양식 업로드</p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 shadow-sm">
                <p class="text-xs font-semibold text-slate-500">02</p>
                <p class="mt-2 text-sm font-semibold text-slate-900">구조 분석</p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 shadow-sm">
                <p class="text-xs font-semibold text-slate-500">03</p>
                <p class="mt-2 text-sm font-semibold text-slate-900">초안 완성</p>
              </div>
            </div>
          </div>
        </section>

        <section v-else class="space-y-6">
          <header class="rounded-2xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
            <div class="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Workspace</p>
                <h2 class="mt-2 text-2xl font-semibold text-slate-950">{{ selectedTab.name }}</h2>
                <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-500">{{ selectedTab.description || '설명 없음' }}</p>
                <p class="mt-3 text-xs font-medium text-slate-400">{{ selectedTab.originalFileName }}</p>
              </div>
              <div class="flex gap-2">
                <button
                  type="button"
                  class="rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300 disabled:cursor-not-allowed disabled:text-slate-400"
                  :disabled="templateAnalysisStore.isLoading || isInteractionLocked"
                  @click="analyzeTemplate"
                >
                  {{ templateAnalysisStore.isLoading ? '분석 중' : '양식 구조 분석' }}
                </button>
                <button
                  type="button"
                  class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
                  :disabled="isInteractionLocked"
                  @click="isTaskModalOpen = true"
                >
                  + 작업 생성
                </button>
              </div>
            </div>
          </header>

          <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h3 class="text-base font-semibold text-slate-950">양식 구조</h3>
                <p class="mt-1 text-sm leading-6 text-slate-500">
                  업로드한 DOCX 양식의 문단과 표 구조를 분석합니다. 향후 AI가 이 구조를 기준으로 입력 내용을 알맞은 위치에 배치합니다.
                </p>
              </div>
              <p v-if="analysisMessage" class="text-sm" :class="isErrorMessage(analysisMessage) ? 'text-red-600' : 'text-emerald-700'">
                {{ analysisMessage }}
              </p>
            </div>

            <div v-if="analysis" class="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_420px]">
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-4">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-semibold text-slate-950">감지된 문서 필드</p>
                    <p class="mt-1 text-xs text-slate-500">문서 구조를 분석해 필드를 자동 추론했습니다. 필요하면 수정하세요.</p>
                    <p class="mt-1 text-xs text-slate-400">향후 AI는 이 매핑 기준으로 문서를 작성합니다.</p>
                  </div>
                  <div class="flex gap-2">
                    <button
                      type="button"
                      class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
                      :disabled="templateAnalysisStore.isRecommendingFields || isInteractionLocked"
                      @click="recommendFields"
                    >
                      {{ templateAnalysisStore.isRecommendingFields ? '개선 중' : 'AI로 필드 추천 개선' }}
                    </button>
                    <button
                      type="button"
                      class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
                      :disabled="isInteractionLocked"
                      @click="openCustomField"
                    >
                      + 사용자 필드 추가
                    </button>
                  </div>
                </div>

                <div v-if="fieldMappings.length" class="mt-4 divide-y divide-slate-200 overflow-hidden rounded-xl border border-slate-200 bg-white">
                  <article v-for="(mapping, index) in fieldMappings" :key="mapping.fieldKey || mapping.id || mapping.sourceLabel" class="px-4 py-4">
                    <div class="flex flex-wrap items-start justify-between gap-3">
                      <div class="min-w-0">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-sm font-semibold text-slate-950">{{ mapping.displayName || mapping.sourceLabel }}</p>
                          <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="confidenceClass(mapping.confidenceLevel)">
                            {{ confidenceLabel(mapping.confidenceLevel) }}
                          </span>
                          <span class="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-600">
                            {{ statusLabel(mapping.mappingStatus) }}
                          </span>
                        </div>
                        <p class="mt-1 text-xs text-slate-400">원본 라벨: {{ mapping.sourceLabel || '연결 없음' }}</p>
                        <p v-if="mapping.description" class="mt-2 text-sm leading-6 text-slate-500">{{ mapping.description }}</p>
                        <p v-if="mapping.mappingStatus === 'CUSTOM' && !mapping.sourceLabel" class="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-700">
                          이 필드는 초안에는 포함되지만 문서 자동 삽입 대상이 아닐 수 있습니다.
                        </p>
                      </div>
                      <div class="flex gap-2">
                        <button
                          type="button"
                          class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
                          :disabled="isInteractionLocked"
                          @click="confirmField(mapping)"
                        >
                          확인
                        </button>
                        <button
                          type="button"
                          class="rounded-lg bg-slate-950 px-3 py-2 text-xs font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                          :disabled="isInteractionLocked"
                          @click="openEditField(mapping, index)"
                        >
                          수정
                        </button>
                      </div>
                    </div>
                  </article>
                </div>
                <p v-else class="mt-4 rounded-xl border border-dashed border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-500">
                  저장할 문서 필드 후보가 없습니다.
                </p>

                <p v-if="mappingMessage" class="mt-3 text-sm" :class="isErrorMessage(mappingMessage) ? 'text-red-600' : 'text-emerald-700'">
                  {{ mappingMessage }}
                </p>
              </div>

              <div class="rounded-xl border border-slate-100 bg-white">
                <div class="flex items-center justify-between border-b border-slate-100 px-4 py-3">
                  <p class="text-sm font-semibold text-slate-950">문서 구조 미리보기</p>
                  <span class="text-xs text-slate-400">{{ analysis.blocks.length }}개 블록</span>
                </div>
                <div class="max-h-[520px] overflow-y-auto p-3">
                  <div v-if="detectedLabels.length" class="mb-3 flex flex-wrap gap-2">
                    <span v-for="label in detectedLabels" :key="`${label.text}-${label.order}`" class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                      {{ label.text }}
                    </span>
                  </div>
                  <div
                    v-for="block in previewBlocks"
                    :key="block.order"
                    class="mb-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-3"
                  >
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="block.type === 'TABLE_CELL' ? 'bg-indigo-50 text-indigo-700' : 'bg-slate-200 text-slate-700'">
                        {{ blockTypeLabel(block.type) }}
                      </span>
                      <span class="text-xs text-slate-400">{{ blockLocation(block) }}</span>
                      <span v-if="block.labelCandidate" class="rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700">
                        라벨 후보
                      </span>
                    </div>
                    <p class="mt-2 text-sm leading-6 text-slate-700">{{ block.text }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <div class="grid min-h-[620px] gap-6 xl:grid-cols-[360px_minmax(0,1fr)_440px]">
            <aside class="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
                <div>
                  <h4 class="text-sm font-semibold text-slate-950">작성 작업</h4>
                  <p class="mt-1 text-xs text-slate-500">{{ taskStore.tasks.length }}개 작업</p>
                </div>
              </div>

              <div v-if="taskStore.isLoading" class="space-y-3 p-4">
                <div class="h-24 animate-pulse rounded-xl bg-slate-100" />
                <div class="h-24 animate-pulse rounded-xl bg-slate-100" />
              </div>

              <div v-else-if="!hasTasks" class="px-6 py-12 text-center">
                <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-sm font-semibold text-slate-700">
                  NEW
                </div>
                <p class="mt-5 text-base font-semibold text-slate-950">아직 작성 작업이 없습니다.</p>
                <p class="mt-2 text-sm leading-6 text-slate-500">새 작업을 만들어 실제 내용을 입력해보세요.</p>
                <button
                  type="button"
                  class="mt-6 rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300"
                  :disabled="isInteractionLocked"
                  @click="isTaskModalOpen = true"
                >
                  작업 생성
                </button>
              </div>

              <div v-else class="space-y-2 p-3">
                <button
                  v-for="task in taskStore.tasks"
                  :key="task.id"
                  type="button"
                  class="w-full rounded-xl border p-4 text-left transition focus:outline-none focus:ring-2 focus:ring-slate-300 focus:ring-offset-2"
                  :class="task.id === taskStore.selectedTaskId ? 'border-slate-900 bg-slate-950 text-white shadow-sm' : 'border-slate-100 bg-white text-slate-800 hover:border-slate-200 hover:bg-slate-50 hover:shadow-sm'"
                  :disabled="isInteractionLocked"
                  @click="selectTask(task.id)"
                >
                  <div class="flex items-start justify-between gap-3">
                    <p class="truncate text-sm font-semibold">{{ task.title }}</p>
                    <span class="rounded-full px-2 py-0.5 text-[11px] font-semibold" :class="task.id === taskStore.selectedTaskId ? 'bg-white/10 text-slate-300' : 'bg-slate-100 text-slate-500'">
                      {{ task.status }}
                    </span>
                  </div>
                  <p class="mt-2 line-clamp-2 text-xs leading-5" :class="task.id === taskStore.selectedTaskId ? 'text-slate-300' : 'text-slate-500'">
                    {{ previewText(task.userContext) }}
                  </p>
                  <p class="mt-3 text-xs" :class="task.id === taskStore.selectedTaskId ? 'text-slate-400' : 'text-slate-400'">
                    {{ formatDate(task.createdAt) }}
                  </p>
                </button>
              </div>
            </aside>

            <section class="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div v-if="!selectedTask" class="flex h-full min-h-[620px] items-center justify-center px-8 py-12 text-center">
                <div class="max-w-md">
                  <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-sm font-semibold text-slate-700">
                    TXT
                  </div>
                  <h4 class="mt-5 text-xl font-semibold text-slate-950">작업을 선택하세요</h4>
                  <p class="mt-2 text-sm leading-6 text-slate-500">작업을 선택하면 입력한 사실과 AI 초안 생성 옵션을 확인할 수 있습니다.</p>
                </div>
              </div>

              <form v-else class="flex h-full min-h-[620px] flex-col" @submit.prevent="saveTask">
                <div class="border-b border-slate-100 px-6 py-5">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Task detail</p>
                      <p class="mt-1 text-sm text-slate-500">{{ formatDate(selectedTask.createdAt) }}</p>
                    </div>
                    <button
                      type="button"
                      class="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-200 disabled:cursor-not-allowed disabled:text-red-300"
                      :disabled="isInteractionLocked"
                      @click="deleteSelectedTask"
                    >
                      삭제
                    </button>
                  </div>
                </div>

                <div class="flex-1 space-y-5 px-6 py-6">
                  <label class="block">
                    <span class="text-sm font-semibold text-slate-800">작업명</span>
                    <input
                      v-model="taskForm.title"
                      type="text"
                      maxlength="200"
                      :disabled="isInteractionLocked"
                      class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    />
                  </label>

                  <label class="block">
                    <span class="text-sm font-semibold text-slate-800">작성 참고 내용</span>
                    <textarea
                      v-model="taskForm.userContext"
                      rows="12"
                      maxlength="8000"
                      :disabled="isInteractionLocked"
                      class="mt-2 min-h-72 w-full resize-y rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    />
                  </label>

                  <div class="rounded-xl border border-slate-100 bg-slate-50 px-4 py-4">
                    <div class="flex items-center justify-between gap-4">
                      <div>
                        <p class="text-sm font-semibold text-slate-900">AI 추론 강도</p>
                        <p class="mt-1 text-xs leading-5 text-slate-500">입력한 사실을 기준으로 얼마나 적극적으로 문장을 보완할지 조절합니다. 없는 사실은 생성하지 않습니다.</p>
                      </div>
                      <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-700 shadow-sm ring-1 ring-slate-200">{{ inferenceStrength }}%</span>
                    </div>
                    <input
                      v-model.number="inferenceStrength"
                      type="range"
                      min="0"
                      max="100"
                      step="10"
                      :disabled="isInteractionLocked"
                      class="mt-4 w-full accent-slate-950 disabled:opacity-50"
                    />
                    <div class="mt-2 flex justify-between text-xs font-medium text-slate-500">
                      <span>보수적</span>
                      <span>균형형</span>
                      <span>적극적</span>
                    </div>
                  </div>
                </div>

                <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-6 py-4">
                  <p class="text-sm" :class="isErrorMessage(saveMessage) ? 'text-red-600' : 'text-emerald-700'">{{ saveMessage }}</p>
                  <div class="flex gap-2">
                    <button
                      type="button"
                      class="rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300 disabled:cursor-not-allowed disabled:text-slate-400"
                      :disabled="isInteractionLocked"
                      @click="generateDraft"
                    >
                      AI 초안 생성
                    </button>
                    <button
                      type="submit"
                      class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
                      :disabled="taskStore.isSaving || isInteractionLocked"
                    >
                      {{ taskStore.isSaving ? '저장 중' : '저장' }}
                    </button>
                  </div>
                </div>
              </form>
            </section>

            <aside class="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div class="border-b border-slate-100 px-6 py-5">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">AI draft</p>
                    <h4 class="mt-2 text-lg font-semibold text-slate-950">AI 초안</h4>
                  </div>
                  <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="generatedDraft ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                    {{ generatedDraft ? '저장된 초안 있음' : '아직 생성된 초안 없음' }}
                  </span>
                </div>
                <p class="mt-3 text-sm leading-6 text-slate-500">생성된 초안을 검토하고 수정한 뒤 저장하세요. 저장된 초안은 이후 DOCX 내보내기에 사용됩니다.</p>
              </div>

              <div v-if="taskStore.isLoadingDraft" class="space-y-3 p-5">
                <div class="h-8 animate-pulse rounded-lg bg-slate-100" />
                <div class="h-80 animate-pulse rounded-xl bg-slate-100" />
              </div>

              <div v-else-if="!generatedDraft" class="flex min-h-[480px] items-center justify-center px-6 py-10 text-center">
                <div>
                  <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-sm font-semibold text-slate-700">
                    AI
                  </div>
                  <p class="mt-5 text-base font-semibold text-slate-950">생성된 초안이 없습니다.</p>
                  <p class="mt-2 text-sm leading-6 text-slate-500">작업 상세에서 AI 초안 생성을 눌러 구조화된 초안을 확인하세요.</p>
                </div>
              </div>

              <div v-else class="p-5">
                <div class="mb-3 flex items-center justify-between gap-3">
                  <span class="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700">구조화 초안</span>
                  <span class="text-xs text-slate-400">수정일 {{ formatDate(generatedDraft.updatedAt) }}</span>
                </div>

                <div v-if="draftFieldEntries.length" class="space-y-3">
                  <section v-for="field in draftFieldEntries" :key="field.key" class="rounded-xl border border-slate-100 bg-slate-50 p-4">
                    <div class="mb-2 flex items-center justify-between gap-3">
                      <label :for="`draft-field-${field.key}`" class="text-sm font-semibold text-slate-900">{{ field.label }}</label>
                    </div>
                    <textarea
                      :id="`draft-field-${field.key}`"
                      v-model="draftFields[field.key]"
                      rows="5"
                      :disabled="isInteractionLocked"
                      class="min-h-32 w-full resize-y rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    />
                  </section>
                </div>

                <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
                  <p class="text-sm" :class="isErrorMessage(draftSaveMessage) ? 'text-red-600' : 'text-emerald-700'">{{ draftSaveMessage }}</p>
                  <div class="flex flex-wrap gap-2">
                    <button type="button" class="rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400" :disabled="taskStore.isLoadingPlacementPreview || !generatedDraft || isInteractionLocked" @click="loadPlacementPreview">
                      {{ taskStore.isLoadingPlacementPreview ? '확인 중' : '배치 미리보기' }}
                    </button>
                    <button type="button" class="rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400" :disabled="taskStore.isLoadingWritePlan || !generatedDraft || isInteractionLocked" @click="loadWritePlan">
                      {{ taskStore.isLoadingWritePlan ? '계획 중' : '삽입 계획 보기' }}
                    </button>
                    <button type="button" class="rounded-lg border border-slate-900 bg-white px-4 py-2.5 text-sm font-semibold text-slate-950 shadow-sm transition hover:bg-slate-950 hover:text-white disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 disabled:hover:bg-white" :disabled="taskStore.isDownloadingDocx || !generatedDraft || isInteractionLocked" @click="downloadCompletedDocx">
                      {{ taskStore.isDownloadingDocx ? '문서 생성 중' : '완성 DOCX 다운로드' }}
                    </button>
                    <button type="button" class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400" :disabled="taskStore.isSavingDraft || !draftFieldEntries.length || isInteractionLocked" @click="saveDraft">
                      {{ taskStore.isSavingDraft ? '저장 중' : '초안 저장' }}
                    </button>
                  </div>
                </div>

                <p v-if="placementMessage" class="mt-3 text-sm" :class="isErrorMessage(placementMessage) ? 'text-red-600' : 'text-emerald-700'">{{ placementMessage }}</p>
                <p v-if="writePlanMessage" class="mt-3 text-sm" :class="isErrorMessage(writePlanMessage) ? 'text-red-600' : 'text-emerald-700'">{{ writePlanMessage }}</p>

                <section class="mt-5 rounded-xl border border-slate-100 bg-white">
                  <div class="border-b border-slate-100 px-4 py-4">
                    <p class="text-sm font-semibold text-slate-950">문서 처리 상태</p>
                    <p class="mt-1 text-xs leading-5 text-slate-500">생성 결과, 배치 미리보기, 삽입 계획을 한 곳에서 확인합니다.</p>
                    <div class="mt-3 grid grid-cols-3 rounded-lg bg-slate-100 p-1 text-xs font-semibold text-slate-600">
                      <button type="button" class="rounded-md px-2 py-2 transition" :class="processingTab === 'summary' ? 'bg-white text-slate-950 shadow-sm' : 'hover:text-slate-950'" @click="processingTab = 'summary'">생성 결과</button>
                      <button type="button" class="rounded-md px-2 py-2 transition" :class="processingTab === 'placement' ? 'bg-white text-slate-950 shadow-sm' : 'hover:text-slate-950'" @click="processingTab = 'placement'">배치 미리보기</button>
                      <button type="button" class="rounded-md px-2 py-2 transition" :class="processingTab === 'plan' ? 'bg-white text-slate-950 shadow-sm' : 'hover:text-slate-950'" @click="processingTab = 'plan'">삽입 계획</button>
                    </div>
                  </div>

                  <div v-if="processingTab === 'summary'" class="p-4">
                    <template v-if="reconstructionSummary?.available">
                      <div class="grid grid-cols-3 gap-3">
                        <div class="rounded-lg bg-slate-50 px-3 py-3">
                          <p class="text-xs font-semibold text-slate-400">전체</p>
                          <p class="mt-1 text-lg font-semibold text-slate-950">{{ reconstructionSummary.totalOperations }}</p>
                        </div>
                        <div class="rounded-lg bg-emerald-50 px-3 py-3">
                          <p class="text-xs font-semibold text-emerald-600">작성 완료</p>
                          <p class="mt-1 text-lg font-semibold text-emerald-700">{{ reconstructionSummary.writtenCount }}</p>
                        </div>
                        <div class="rounded-lg bg-amber-50 px-3 py-3">
                          <p class="text-xs font-semibold text-amber-600">스킵</p>
                          <p class="mt-1 text-lg font-semibold text-amber-700">{{ reconstructionSummary.skippedCount }}</p>
                        </div>
                      </div>
                      <p class="mt-3 text-xs leading-5 text-slate-500">일부 항목은 양식 구조상 자동 삽입되지 않았을 수 있습니다. 스킵된 항목은 문서에서 직접 확인하거나 양식 매핑을 조정하세요.</p>
                      <div v-if="skippedReconstructionResults.length" class="mt-3 divide-y divide-slate-100 rounded-lg border border-slate-100">
                        <article v-for="result in skippedReconstructionResults" :key="`${result.semanticFieldKey}-${result.sourceLabel}-${result.status}`" class="px-3 py-3">
                          <div class="flex items-start justify-between gap-3">
                            <p class="text-sm font-semibold text-slate-950">{{ result.displayName || result.sourceLabel }}</p>
                            <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="reconstructionStatusClass(result.status)">{{ reconstructionStatusLabel(result.status) }}</span>
                          </div>
                          <p class="mt-2 text-xs leading-5 text-slate-500">{{ result.message }}</p>
                        </article>
                      </div>
                    </template>
                    <p v-else class="rounded-lg border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500">아직 문서 생성 결과가 없습니다.</p>
                  </div>

                  <div v-else-if="processingTab === 'placement'" class="divide-y divide-slate-100">
                    <article v-for="row in placementPreview" :key="`${row.semanticFieldKey}-${row.sourceLabel}`" class="px-4 py-4">
                      <div class="flex items-start justify-between gap-3">
                        <p class="text-sm font-semibold text-slate-950">{{ row.displayName }}</p>
                        <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="placementStatusClass(row.status)">{{ placementStatusLabel(row.status) }}</span>
                      </div>
                      <p class="mt-2 text-sm leading-6 text-slate-500"><span class="font-semibold text-slate-700">양식 위치:</span> {{ row.targetText || row.sourceLabel || '대상 위치 없음' }}</p>
                      <p class="mt-1 line-clamp-2 text-sm leading-6 text-slate-500"><span class="font-semibold text-slate-700">삽입 예정 내용:</span> {{ row.draftValue || '추가 입력 필요' }}</p>
                    </article>
                    <p v-if="!placementPreview.length" class="px-4 py-8 text-center text-sm text-slate-500">배치 미리보기를 실행하면 결과가 표시됩니다.</p>
                  </div>

                  <div v-else class="divide-y divide-slate-100">
                    <article v-for="operation in writePlan" :key="`${operation.semanticFieldKey}-${operation.sourceLabel}`" class="px-4 py-4">
                      <div class="flex items-start justify-between gap-3">
                        <p class="text-sm font-semibold text-slate-950">{{ operation.displayName }}</p>
                        <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="placementStatusClass(operation.status)">{{ placementStatusLabel(operation.status) }}</span>
                      </div>
                      <p class="mt-2 text-sm leading-6 text-slate-500"><span class="font-semibold text-slate-700">작업 방식:</span> {{ operationTypeLabel(operation.operationType) }}</p>
                      <p class="mt-1 text-sm leading-6 text-slate-500"><span class="font-semibold text-slate-700">대상 위치:</span> {{ writePlanLocation(operation) }}</p>
                      <p class="mt-1 line-clamp-2 text-sm leading-6 text-slate-500"><span class="font-semibold text-slate-700">값 미리보기:</span> {{ operation.value || '추가 입력 필요' }}</p>
                    </article>
                    <p v-if="!writePlan.length" class="px-4 py-8 text-center text-sm text-slate-500">삽입 계획을 실행하면 결과가 표시됩니다.</p>
                  </div>
                </section>
              </div>
            </aside>
          </div>
        </section>
      </div>
    </main>

    <TabCreateModal v-if="isCreateModalOpen" :is-submitting="isCreatingTab" :submit-error="tabModalErrorMessage" @close="isCreateModalOpen = false" @submit="createTab" />
    <TaskCreateModal v-if="isTaskModalOpen" :is-submitting="taskStore.isSaving" :submit-error="taskModalErrorMessage" @close="isTaskModalOpen = false" @submit="createTask" />

    <div v-if="tabDeleteCandidate" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/40 px-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <section class="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl shadow-slate-950/20">
        <h3 class="text-lg font-semibold text-slate-950">탭 삭제</h3>
        <p class="mt-3 text-sm leading-6 text-slate-600">이 탭과 관련된 작업, 초안, 분석 결과, 생성 문서 정보를 모두 삭제하시겠습니까?</p>
        <p v-if="tabDeleteMessage" class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{{ tabDeleteMessage }}</p>
        <div class="mt-6 flex justify-end gap-3">
          <button type="button" class="rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50" @click="cancelDeleteTab">취소</button>
          <button type="button" class="rounded-lg bg-red-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-red-300" :disabled="tabStore.isDeleting" @click="confirmDeleteTab">
            {{ tabStore.isDeleting ? '삭제 중' : '삭제' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="fieldEditModalOpen" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/40 px-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl shadow-slate-950/20">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-950">{{ fieldEditMode === 'custom' ? '사용자 필드 추가' : '문서 필드 수정' }}</h3>
            <p class="mt-1 text-sm text-slate-500">필드 이름과 설명은 AI 초안 품질에 직접 영향을 줍니다.</p>
          </div>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900" @click="closeFieldEditModal">x</button>
        </div>

        <div class="mt-5 space-y-4">
          <label class="block">
            <span class="text-sm font-semibold text-slate-800">필드 이름</span>
            <input v-model="fieldEditForm.displayName" type="text" class="mt-2 w-full rounded-lg border border-slate-200 px-3.5 py-3 text-sm outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100" />
          </label>
          <label class="block">
            <span class="text-sm font-semibold text-slate-800">설명</span>
            <textarea v-model="fieldEditForm.description" rows="3" class="mt-2 w-full resize-none rounded-lg border border-slate-200 px-3.5 py-3 text-sm leading-6 outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100" />
          </label>
          <label class="flex items-center gap-2 text-sm font-medium text-slate-700">
            <input v-model="fieldEditForm.required" type="checkbox" class="rounded border-slate-300 text-slate-950 focus:ring-slate-400" />
            필수 항목
          </label>
          <label class="block">
            <span class="text-sm font-semibold text-slate-800">DOCX 원본 라벨 연결</span>
            <input v-model="fieldEditForm.sourceLabel" type="text" placeholder="비워두면 초안 전용 필드가 됩니다." class="mt-2 w-full rounded-lg border border-slate-200 px-3.5 py-3 text-sm outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100" />
            <span v-if="fieldEditMode === 'custom' && !fieldEditForm.sourceLabel" class="mt-2 block text-xs leading-5 text-amber-700">이 필드는 초안에는 포함되지만 문서 자동 삽입 대상이 아닐 수 있습니다.</span>
          </label>

          <button type="button" class="text-sm font-semibold text-slate-700" @click="fieldAdvancedOpen = !fieldAdvancedOpen">
            고급 설정 {{ fieldAdvancedOpen ? '닫기' : '열기' }}
          </button>
          <div v-if="fieldAdvancedOpen" class="space-y-4 rounded-xl border border-slate-100 bg-slate-50 p-4">
            <label class="block">
              <span class="text-sm font-semibold text-slate-800">작성 규칙</span>
              <textarea v-model="fieldEditForm.writingRule" rows="3" class="mt-2 w-full resize-none rounded-lg border border-slate-200 px-3.5 py-3 text-sm leading-6 outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100" />
            </label>
            <label class="block">
              <span class="text-sm font-semibold text-slate-800">system fieldKey</span>
              <input v-model="fieldEditForm.fieldKey" type="text" readonly class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-500" />
            </label>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-5">
          <button type="button" class="rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50" @click="closeFieldEditModal">취소</button>
          <button type="button" class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400" :disabled="!fieldEditForm.displayName.trim() || isInteractionLocked" @click="saveFieldEdit">저장</button>
        </div>
      </section>
    </div>

    <div v-if="successToastMessage" class="fixed bottom-6 right-6 z-50 rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white shadow-xl">
      {{ successToastMessage }}
    </div>

    <div v-if="taskStore.isGenerating" class="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 px-4 backdrop-blur-sm" aria-live="polite">
      <section class="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 text-center shadow-2xl shadow-slate-950/20">
        <div class="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-slate-950" />
        <h3 class="mt-5 text-lg font-semibold text-slate-950">AI가 문서를 생성하고 있습니다</h3>
        <p class="mt-2 text-sm text-slate-500">{{ activeGenerationStep }}</p>
        <div class="mt-5 space-y-2 text-left">
          <div v-for="(step, index) in generationProgressSteps" :key="step" class="flex items-center gap-3 rounded-lg px-3 py-2" :class="index <= taskStore.generationStepIndex ? 'bg-slate-100 text-slate-900' : 'text-slate-400'">
            <span class="flex h-5 w-5 items-center justify-center rounded-full text-[11px] font-semibold" :class="index <= taskStore.generationStepIndex ? 'bg-slate-950 text-white' : 'bg-slate-200 text-slate-500'">{{ index + 1 }}</span>
            <span class="text-sm font-medium">{{ step }}</span>
          </div>
        </div>
        <p v-if="taskStore.generationDelayWarning" class="mt-5 rounded-lg bg-amber-50 px-3 py-2 text-sm font-medium text-amber-700">AI 응답이 지연되고 있습니다. 잠시만 기다려주세요.</p>
      </section>
    </div>
  </div>
</template>
