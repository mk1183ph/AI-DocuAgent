<script setup>
import { reactive, ref } from 'vue'

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
  title: '',
  userContext: ''
})

const errorMessage = ref('')

function submit() {
  errorMessage.value = ''

  if (!form.title.trim()) {
    errorMessage.value = '작업명을 입력하세요.'
    return
  }

  if (!form.userContext.trim()) {
    errorMessage.value = '작성 참고 내용을 입력하세요.'
    return
  }

  emit('submit', {
    title: form.title.trim(),
    userContext: form.userContext.trim()
  })
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 backdrop-blur-sm">
    <section class="w-full max-w-2xl rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-950/20">
      <div class="flex items-start justify-between gap-4">
        <div class="px-6 pt-6">
          <h2 class="text-xl font-semibold text-slate-950">새 작업 생성</h2>
          <p class="mt-1 text-sm text-slate-500">실제 사실을 입력해 문서 작성 준비를 시작합니다.</p>
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
          <span class="text-sm font-semibold text-slate-800">작업명</span>
          <input
            v-model="form.title"
            type="text"
            maxlength="200"
            placeholder="예: 5월 관찰 기록"
            class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
        </label>

        <label class="block">
          <span class="text-sm font-semibold text-slate-800">작성 참고 내용</span>
          <textarea
            v-model="form.userContext"
            rows="12"
            maxlength="8000"
            placeholder="오늘 실제 활동 내용을 자유롭게 입력하세요.&#10;AI는 입력한 사실만 바탕으로 문서를 작성합니다."
            class="mt-2 min-h-72 w-full resize-y rounded-lg border border-slate-200 bg-white px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
        </label>

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
