# Ollama Runtime Smoke Guide

Phase 12 validates real local AI inference through Ollama. Mock generation remains available only as a development fallback.

## Expected Local Setup

- Ollama is installed and running.
- Ollama API is available at `http://localhost:11434`.
- The configured model exists locally.
- Default model: `qwen2.5:7b`.

## Start Ollama

```powershell
ollama serve
```

In a separate terminal, pull the default model if needed:

```powershell
ollama pull qwen2.5:7b
```

Check available models:

```powershell
Invoke-RestMethod http://localhost:11434/api/tags
```

## App Settings

Open the app AI settings screen and confirm:

- AI Provider: `Ollama`
- Ollama URL: `http://localhost:11434`
- Model Name: `qwen2.5:7b`
- Timeout: `300`

## Focused Runtime Smoke Flow

1. Upload a DOCX template.
2. Analyze the template.
3. Confirm or edit semantic mappings.
4. Create a task with factual context.
5. Click `AI 초안 생성`.
6. Confirm the draft is structured by mapped fields.
7. Confirm missing facts are shown as `추가 입력 필요` or `미기재`.

## Expected Failure Messages

- Ollama not running: `Ollama가 실행 중인지 확인해주세요.`
- Missing model: `설정된 모델을 찾을 수 없습니다.`
- Timeout: `요청 시간이 초과되었습니다.`
- Invalid AI output: `AI 응답을 JSON으로 해석하지 못했습니다.`
