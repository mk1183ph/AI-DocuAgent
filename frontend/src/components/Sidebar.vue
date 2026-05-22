<script setup>
import { computed } from 'vue'

const props = defineProps({
  tabs: {
    type: Array,
    required: true
  },
  selectedTabId: {
    type: Number,
    default: null
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  isLocked: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['create-tab', 'select-tab', 'delete-tab'])

const hasTabs = computed(() => props.tabs.length > 0)
</script>

<template>
  <aside class="flex h-full w-80 shrink-0 flex-col border-r border-slate-200/80 bg-white/95 shadow-[12px_0_40px_rgba(15,23,42,0.04)]">
    <div class="border-b border-slate-200/80 px-5 py-5">
      <div class="flex items-start gap-3">
        <div class="relative flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600 via-violet-600 to-sky-500 text-sm font-black text-white shadow-lg shadow-indigo-500/20 ring-1 ring-white/60">
          <span class="tracking-tight">AI</span>
          <span class="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full border-2 border-white bg-amber-800 shadow-sm">
            <svg class="h-3.5 w-3.5 text-amber-100" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M6.4 8.6 8 5l3 2.2h2L16 5l1.6 3.6" fill="currentColor" opacity="0.9" />
              <path d="M5.8 11.4c0-4.1 12.4-4.1 12.4 0 0 4.8-2.8 7.1-6.2 7.1s-6.2-2.3-6.2-7.1Z" fill="currentColor" />
              <path d="M9.2 11.8h.1M14.7 11.8h.1" stroke="#78350f" stroke-width="2" stroke-linecap="round" />
              <path d="M12 13.4v1.2" stroke="#78350f" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
        </div>
        <div class="min-w-0">
          <h1 class="text-base font-semibold tracking-tight text-slate-950">AI DocuAgent</h1>
          <p class="mt-0.5 text-xs font-medium text-slate-500">문서 자동화 워크스페이스</p>
          <p class="mt-1 text-[11px] font-semibold text-violet-600">Made By 뽈뽀리 💜</p>
        </div>
      </div>

      <button
        type="button"
        class="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 via-violet-600 to-sky-500 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-indigo-500/20 transition duration-150 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-indigo-500/25 focus:outline-none focus:ring-2 focus:ring-violet-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0 disabled:hover:shadow-lg"
        :disabled="isLocked"
        @click="emit('create-tab')"
      >
        <span class="text-base leading-none">+</span>
        탭 추가
      </button>

      <RouterLink
        to="/settings"
        class="mt-2 flex w-full items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-violet-200 hover:bg-violet-50/60 hover:text-violet-700 focus:outline-none focus:ring-2 focus:ring-violet-200"
        :class="isLocked ? 'pointer-events-none opacity-50' : ''"
      >
        AI 설정
      </RouterLink>
    </div>

    <nav class="flex-1 overflow-y-auto px-3 py-4">
      <div v-if="isLoading" class="space-y-2 px-2">
        <div class="h-16 animate-pulse rounded-xl bg-slate-100" />
        <div class="h-16 animate-pulse rounded-xl bg-slate-100" />
      </div>

      <div v-else-if="!hasTabs" class="rounded-2xl border border-dashed border-violet-200 bg-gradient-to-br from-violet-50 to-sky-50 px-4 py-5">
        <p class="text-sm font-semibold text-slate-800">아직 업무 탭이 없습니다.</p>
        <p class="mt-1 text-xs leading-5 text-slate-500">DOCX 양식을 업로드해 문서 자동화 워크스페이스를 시작하세요.</p>
      </div>

      <div
        v-for="tab in tabs"
        :key="tab.id"
        class="group mb-2 flex w-full items-start gap-2 rounded-2xl border px-3 py-3 text-left transition duration-150 focus-within:ring-2 focus-within:ring-violet-300 focus-within:ring-offset-2"
        :class="tab.id === selectedTabId ? 'border-violet-500/70 bg-slate-950 text-white shadow-lg shadow-slate-900/10' : 'border-transparent bg-white text-slate-700 hover:border-violet-100 hover:bg-violet-50/50 hover:shadow-sm'"
      >
        <button
          type="button"
          class="min-w-0 flex-1 text-left focus:outline-none disabled:cursor-not-allowed"
          :disabled="isLocked"
          @click="emit('select-tab', tab.id)"
        >
          <span class="block truncate text-sm font-semibold">{{ tab.name }}</span>
          <span class="mt-1 block truncate text-xs" :class="tab.id === selectedTabId ? 'text-slate-300' : 'text-slate-500'">
            {{ tab.description || '설명 없음' }}
          </span>
          <span class="mt-2 flex items-center gap-1.5 text-xs" :class="tab.id === selectedTabId ? 'text-slate-300' : 'text-slate-500'">
            <span class="h-1.5 w-1.5 rounded-full" :class="tab.id === selectedTabId ? 'bg-sky-300' : 'bg-emerald-500'" />
            <span class="truncate">{{ tab.originalFileName || 'DOCX 미등록' }}</span>
          </span>
        </button>

        <button
          type="button"
          class="mt-0.5 rounded-lg p-1.5 opacity-0 transition hover:bg-red-50 hover:text-red-600 focus:opacity-100 focus:outline-none focus:ring-2 focus:ring-red-200 group-hover:opacity-100 disabled:cursor-not-allowed disabled:opacity-30"
          :class="tab.id === selectedTabId ? 'text-slate-300 hover:bg-white/10 hover:text-white' : 'text-slate-400'"
          :disabled="isLocked"
          aria-label="탭 삭제"
          title="탭 삭제"
          @click.stop="emit('delete-tab', tab)"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 7h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M10 11v6M14 11v6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M6.5 7l.7 12.2A2 2 0 0 0 9.2 21h5.6a2 2 0 0 0 2-1.8L17.5 7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M9 7V5.8A1.8 1.8 0 0 1 10.8 4h2.4A1.8 1.8 0 0 1 15 5.8V7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </button>
      </div>
    </nav>
  </aside>
</template>
