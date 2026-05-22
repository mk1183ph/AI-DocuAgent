# Windows Local Run Guide

DocuAgent Local is a local-first Windows desktop web application during MVP development. It runs as a Spring Boot backend with a Vue frontend in dev mode, and can also be packaged into one Spring Boot jar with the frontend embedded.

## Requirements

- Windows 10 or newer
- Java 17 JDK
- Node.js 18 or newer for frontend development and local packaging
- Ollama for real local AI inference
- Ollama model: `qwen2.5:7b`

Check Java:

```bat
java -version
```

The version must be Java 17.

Install or prepare Ollama:

```bat
ollama pull qwen2.5:7b
ollama list
```

Ollama should be available at:

```text
http://localhost:11434
```

## First Run Directories

The app uses local filesystem storage:

- `storage\app.db`
- `storage\generated\`
- `templates\`

The startup scripts create these folders if they are missing. The backend also creates required storage folders on startup.

## Development Mode

Start backend and frontend in separate terminal windows:

```bat
run-dev.bat
```

Or start them manually:

```bat
run-backend.bat
run-frontend.bat
```

Development URLs:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/api/health`

## Production-Style Local Jar

Build the Vue frontend, copy it into Spring Boot static resources, build the backend jar, and run the app from one Spring Boot process:

```bat
run-all.bat
```

The app will be available at:

```text
http://localhost:8080
```

The Gradle packaging task used by `run-all.bat` is:

```bat
cd backend
gradlew.bat packageLocal
```

This task:

1. Runs `npm run build` in `frontend`.
2. Copies `frontend\dist` into Spring Boot static resources.
3. Builds a Spring Boot jar under `backend\build\libs\`.

## Common Troubleshooting

### Java version is wrong

Install Java 17 and make sure `java -version` shows Java 17 before running Gradle or the jar.

### Frontend dependencies are missing

`run-frontend.bat` installs dependencies if `frontend\node_modules` is missing. If install fails, run:

```bat
cd frontend
npm install
```

### Ollama is not running

Start Ollama and confirm the API responds:

```bat
ollama list
```

Then open:

```text
http://localhost:11434/api/tags
```

### Model is missing

Pull the default model:

```bat
ollama pull qwen2.5:7b
```

### AI generation times out

The default local request timeout is 300 seconds. If your machine is slower, increase the timeout in the app settings screen.

### Port already in use

The backend uses port `8080`. The dev frontend uses port `5173`.

Find the process using a port:

```bat
netstat -ano | findstr :8080
netstat -ano | findstr :5173
```

Stop the process from Task Manager or choose a different port for local testing.

### Health check fails

Check:

```text
http://localhost:8080/api/health
```

Expected status:

```json
{
  "status": "UP",
  "application": "docuagent-local"
}
```
