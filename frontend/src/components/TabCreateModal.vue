<script setup>
import { computed, reactive, ref } from 'vue'

defineProps({
  isSubmitting: {
    type: Boolean,
    default: false
  },
  submitError: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'submit'])

const form = reactive({
  name: '',
  description: '',
  basePrompt: '',
  templateFile: null
})

const errorMessage = ref('')
const isDragging = ref(false)

const selectedFileName = computed(() => form.templateFile?.name ?? '')

function selectFile(file) {
  errorMessage.value = ''

  if (!file) {
    form.templateFile = null
    return
  }

  if (!file.name.toLowerCase().endsWith('.docx')) {
    form.templateFile = null
    errorMessage.value = 'DOCX 파일만 업로드할 수 있습니다.'
    return
  }

  form.templateFile = file
}

function handleFileChange(event) {
  selectFile(event.target.files?.[0])
}

function handleDrop(event) {
  isDragging.value = false
  selectFile(event.dataTransfer.files?.[0])
}

function submit() {
  errorMessage.value = ''

  if (!form.name.trim()) {
    errorMessage.value = '탭 이름을 입력하세요.'
    return
  }

  if (!form.templateFile) {
    errorMessage.value = 'DOCX 양식 파일을 업로드하세요.'
    return
  }

  emit('submit', {
    name: form.name.trim(),
    description: form.description.trim(),
    basePrompt: form.basePrompt.trim(),
    templateFile: form.templateFile
  })
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 backdrop-blur-sm">
    <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-950/20">
      <div class="flex items-start justify-between gap-4">
        <div class="px-6 pt-6">
          <h2 class="text-xl font-semibold text-slate-950">새 업무 탭 만들기</h2>
          <p class="mt-1 text-sm text-slate-500">기존 DOCX 양식을 업로드해 문서 작성 공간을 준비합니다.</p>
        </div>
        <button
          type="button"
          class="mr-4 mt-4 rounded-lg px-2.5 py-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-300"
          aria-label="닫기"
          @click="emit('close')"
        >
          x
        </button>
      </div>

      <form class="mt-6 space-y-5 px-6 pb-6" @submit.prevent="submit">
        <label class="block">
          <span class="text-sm font-semibold text-slate-800">탭 이름</span>
          <input
            v-model="form.name"
            type="text"
            maxlength="100"
            placeholder="예: 관찰 기록지"
            class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
        </label>

        <label class="block">
          <span class="text-sm font-semibold text-slate-800">설명</span>
          <textarea
            v-model="form.description"
            rows="3"
            maxlength="1000"
            placeholder="이 양식의 용도나 문서 유형을 간단히 적어주세요."
            class="mt-2 w-full resize-none rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
        </label>

        <label class="block">
          <span class="text-sm font-semibold text-slate-800">기본 작성 규칙</span>
          <textarea
            v-model="form.basePrompt"
            rows="3"
            maxlength="4000"
            placeholder="예: 공손하고 간결한 문체로 작성"
            class="mt-2 w-full resize-none rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
        </label>

        <div>
          <span class="text-sm font-semibold text-slate-800">DOCX 파일 업로드</span>
          <label
            class="mt-2 flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed px-5 py-7 text-center transition"
            :class="isDragging ? 'border-slate-500 bg-slate-100' : 'border-slate-300 bg-slate-50 hover:border-slate-400 hover:bg-white'"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <input type="file" accept=".docx" class="sr-only" @change="handleFileChange" />
            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-lg font-semibold text-slate-700 shadow-sm">
              DOC
            </span>
            <span class="mt-3 text-sm font-semibold text-slate-800">파일을 끌어오거나 클릭해 선택</span>
            <span class="mt-1 text-xs text-slate-500">.docx 형식만 지원합니다.</span>
            <span v-if="selectedFileName" class="mt-3 max-w-full truncate rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
              {{ selectedFileName }}
            </span>
          </label>
        </div>

        <p v-if="errorMessage || submitError" class="rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
          {{ errorMessage || submitError }}
        </p>

        <div class="flex justify-end gap-3 border-t border-slate-100 pt-5">
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300"
            @click="emit('close')"
          >
            취소
          </button>
          <button
            type="submit"
            class="rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? '저장 중' : '저장' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
