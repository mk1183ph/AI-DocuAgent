<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '@/components/Sidebar.vue'
import { useSettingsStore } from '@/stores/settingsStore'
import { useTabStore } from '@/stores/tabStore'

const settingsStore = useSettingsStore()
const tabStore = useTabStore()
const router = useRouter()
const message = ref('')

const form = reactive({
  aiProvider: 'OLLAMA',
  aiWritingMode: 'BALANCED',
  ollamaBaseUrl: 'http://localhost:11434',
  ollamaModel: 'qwen2.5:7b',
  geminiApiKey: '',
  geminiModel: 'gemini-2.5-flash',
  requestTimeoutSeconds: 300
})

onMounted(async () => {
  tabStore.loadTabs()
  try {
    await settingsStore.loadSettings()
    syncForm(settingsStore.settings)
  } catch (error) {
    message.value = settingsStore.errorMessage
  }
})

watch(
  () => settingsStore.settings,
  (settings) => syncForm(settings),
  { deep: true }
)

function syncForm(settings) {
  if (!settings) {
    return
  }

  form.aiProvider = settings.aiProvider ?? 'OLLAMA'
  form.aiWritingMode = settings.aiWritingMode ?? 'BALANCED'
  form.ollamaBaseUrl = settings.ollamaBaseUrl ?? 'http://localhost:11434'
  form.ollamaModel = settings.ollamaModel ?? 'qwen2.5:7b'
  form.geminiApiKey = settings.geminiApiKey ?? ''
  form.geminiModel = settings.geminiModel ?? 'gemini-2.5-flash'
  form.requestTimeoutSeconds = settings.requestTimeoutSeconds ?? 300
}

async function saveSettings() {
  message.value = ''
  try {
    await settingsStore.saveSettings({
      aiProvider: form.aiProvider,
      aiWritingMode: form.aiWritingMode,
      ollamaBaseUrl: form.ollamaBaseUrl.trim(),
      ollamaModel: form.ollamaModel.trim(),
      geminiApiKey: form.geminiApiKey.trim(),
      geminiModel: form.geminiModel.trim(),
      requestTimeoutSeconds: Number(form.requestTimeoutSeconds)
    })
    message.value = 'AI 설정을 저장했습니다.'
  } catch (error) {
    message.value = settingsStore.errorMessage || 'AI 설정을 저장하지 못했습니다.'
  }
}

function selectTab(tabId) {
  tabStore.selectTab(tabId)
  router.push('/')
}

function isErrorMessage(value) {
  return value.includes('못했습니다') || value.includes('확인해주세요') || value.includes('입력해주세요')
}
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-slate-100">
    <Sidebar
      :tabs="tabStore.tabs"
      :selected-tab-id="tabStore.selectedTabId"
      :is-loading="tabStore.isLoading"
      @create-tab="$router.push('/')"
      @select-tab="selectTab"
    />

    <main class="min-w-0 flex-1 overflow-y-auto">
      <div class="mx-auto flex min-h-full max-w-5xl flex-col px-8 py-8">
        <header class="flex items-start justify-between gap-6">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Local AI engine</p>
            <h2 class="mt-2 text-2xl font-semibold text-slate-950">AI 설정</h2>
            <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
              문서 초안 생성에 사용할 AI 제공자를 선택합니다. 로컬 Ollama를 사용하면 문서 내용이 외부 서버로 전송되지 않습니다.
            </p>
          </div>
          <RouterLink
            to="/"
            class="rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300"
          >
            작업 화면으로
          </RouterLink>
        </header>

        <section class="mt-8 rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div class="border-b border-slate-100 px-7 py-6">
            <h3 class="text-lg font-semibold text-slate-950">AI 제공자</h3>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              실제 초안 생성은 선택한 제공자를 통해 실행됩니다. Mock은 개발 확인용 fallback으로만 사용하세요.
            </p>
          </div>

          <form class="space-y-6 px-7 py-6" @submit.prevent="saveSettings">
            <label class="block">
              <span class="text-sm font-semibold text-slate-800">AI Provider</span>
              <select
                v-model="form.aiProvider"
                class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
              >
                <option value="OLLAMA">Ollama</option>
                <option value="GEMINI">Gemini</option>
                <option value="MOCK">Mock</option>
              </select>
            </label>

            <section class="rounded-xl border border-slate-100 bg-slate-50 px-5 py-5">
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p class="text-sm font-semibold text-slate-900">AI 작성 모드</p>
                  <p class="mt-2 text-sm leading-6 text-slate-500">
                    입력 사실을 얼마나 보수적으로 해석할지 선택합니다. 기본값은 균형형입니다.
                  </p>
                </div>
                <select
                  v-model="form.aiWritingMode"
                  class="w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm font-semibold text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100 md:w-56"
                >
                  <option value="CONSERVATIVE">보수적</option>
                  <option value="BALANCED">균형형</option>
                  <option value="AGGRESSIVE">적극적</option>
                </select>
              </div>
              <div class="mt-4 grid gap-3 md:grid-cols-3">
                <div class="rounded-lg bg-white px-4 py-3 ring-1 ring-slate-200">
                  <p class="text-xs font-semibold text-slate-900">보수적</p>
                  <p class="mt-1 text-xs leading-5 text-slate-500">입력한 사실만 기반으로 작성합니다. 부족한 정보는 비워 둡니다.</p>
                </div>
                <div class="rounded-lg bg-white px-4 py-3 ring-1 ring-slate-200">
                  <p class="text-xs font-semibold text-slate-900">균형형</p>
                  <p class="mt-1 text-xs leading-5 text-slate-500">입력 사실을 바탕으로 합리적으로 내용을 정리하고 보완합니다.</p>
                </div>
                <div class="rounded-lg bg-white px-4 py-3 ring-1 ring-slate-200">
                  <p class="text-xs font-semibold text-slate-900">적극적</p>
                  <p class="mt-1 text-xs leading-5 text-slate-500">입력 내용을 적극적으로 확장합니다. 결과 검토가 필요합니다.</p>
                </div>
              </div>
            </section>

            <div v-if="form.aiProvider === 'OLLAMA'" class="space-y-5">
              <div class="rounded-xl border border-emerald-100 bg-emerald-50 px-5 py-4">
                <p class="text-sm font-semibold text-emerald-800">로컬 AI 보호 안내</p>
                <p class="mt-2 text-sm leading-6 text-emerald-700">
                  Ollama 사용 시 문서 내용이 로컬 PC 밖으로 전송되지 않습니다.
                </p>
              </div>

              <div class="grid gap-5 md:grid-cols-2">
                <label class="block">
                  <span class="text-sm font-semibold text-slate-800">Ollama URL</span>
                  <input
                    v-model="form.ollamaBaseUrl"
                    type="url"
                    class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    placeholder="http://localhost:11434"
                  />
                </label>

                <label class="block">
                  <span class="text-sm font-semibold text-slate-800">Model Name</span>
                  <input
                    v-model="form.ollamaModel"
                    type="text"
                    class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    placeholder="qwen2.5:7b"
                  />
                </label>
              </div>
            </div>

            <div v-if="form.aiProvider === 'GEMINI'" class="space-y-5">
              <div class="rounded-xl border border-amber-100 bg-amber-50 px-5 py-4">
                <p class="text-sm font-semibold text-amber-800">외부 API 전송 안내</p>
                <p class="mt-2 text-sm leading-6 text-amber-700">
                  Gemini 사용 시 문서 내용이 Google Gemini API로 전송됩니다.
                </p>
              </div>

              <div class="grid gap-5 md:grid-cols-2">
                <label class="block">
                  <span class="text-sm font-semibold text-slate-800">Gemini API Key</span>
                  <input
                    v-model="form.geminiApiKey"
                    type="password"
                    autocomplete="off"
                    class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    placeholder="Google AI Studio API 키"
                  />
                </label>

                <label class="block">
                  <span class="text-sm font-semibold text-slate-800">Gemini Model</span>
                  <input
                    v-model="form.geminiModel"
                    type="text"
                    class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                    placeholder="gemini-2.5-flash"
                  />
                </label>
              </div>
            </div>

            <div v-if="form.aiProvider === 'MOCK'" class="rounded-xl border border-amber-100 bg-amber-50 px-5 py-4">
              <p class="text-sm font-semibold text-amber-800">Mock 제공자</p>
              <p class="mt-2 text-sm leading-6 text-amber-700">
                Mock은 개발 확인용입니다. 실제 문서 초안 품질 검증에는 Ollama 또는 Gemini를 사용하세요.
              </p>
            </div>

            <label v-if="form.aiProvider !== 'MOCK'" class="block max-w-xs">
              <span class="text-sm font-semibold text-slate-800">Timeout</span>
              <div class="mt-2 flex items-center gap-3">
                <input
                  v-model.number="form.requestTimeoutSeconds"
                  type="number"
                  min="5"
                  max="600"
                  class="w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
                />
                <span class="text-sm font-medium text-slate-500">초</span>
              </div>
            </label>

            <div class="rounded-xl border border-slate-100 bg-slate-50 px-5 py-4">
              <p class="text-sm font-semibold text-slate-900">응답 형식</p>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                AI는 저장된 문서 필드 매핑을 기준으로 JSON만 반환해야 하며, 입력되지 않은 사실은 “추가 입력 필요” 또는 “미기재”로 표시합니다.
              </p>
            </div>

            <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5">
              <p
                v-if="message"
                class="text-sm"
                :class="isErrorMessage(message) ? 'text-red-600' : 'text-emerald-700'"
              >
                {{ message }}
              </p>
              <span v-else class="text-sm text-slate-400">
                기본 제공자는 Ollama입니다.
              </span>
              <button
                type="submit"
                class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="settingsStore.isSaving"
              >
                {{ settingsStore.isSaving ? '저장 중' : '설정 저장' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>
  </div>
</template>
