# Local AI development platform

Java 21, Spring Boot 3.4.5, Spring AI 1.0.0, and Temporal Java SDK 1.26.2. Requirements arrive through REST; no generated project type or filename is built into the platform. The five stages are requirements, architecture, development, review, and QA. Each stage is a Temporal Activity, and each waits for a human decision when `approvalRequired` is `true`.

For the request-to-completion sequence, approval decisions, revisions, and file locations, see [How the workflow works](docs/WORKFLOW.md).

## Java package layout

| Package | Responsibility |
|---|---|
| `local.aidev.api` | REST endpoints and JSON error responses |
| `local.aidev.workflow` | Temporal supervisor and approval gates |
| `local.aidev.activity` | Temporal Activity contracts and implementations |
| `local.aidev.agent` | Spring AI prompts and agent output handling |
| `local.aidev.workspace` | Safe local project file operations |
| `local.aidev.persistence` | Project metadata, status, and history |
| `local.aidev.config` | Temporal worker and client configuration |
| `local.aidev.model` | Shared request, decision, status, and artifact types |

`PlatformApplication` stays in `local.aidev` so Spring scans all subpackages.

## Windows PowerShell setup

Install Java 21, Maven, and Docker Desktop. Start Docker Desktop, then run this in one PowerShell window:

```powershell
docker run --rm --name temporal-dev -p 7233:7233 -p 8233:8233 -v temporal-dev-data:/var/lib/temporal temporalio/temporal:latest server start-dev --ip 0.0.0.0 --db-filename /var/lib/temporal/temporal.db
```

Temporal UI: <http://localhost:8233>. In another PowerShell window, from this repository:

```powershell
$env:AI_WORKSPACE = 'C:\AI-Development\workspace'
$env:TEMPORAL_ADDRESS = '127.0.0.1:7233'
mvn clean package
java -jar .\target\ai-development-platform-1.0.0.jar
```

The server listens on port 8081. Set `MAX_REVISIONS` to change the default revision limit. The server creates the configured workspace root. The project directory name comes from the validated `projectName`; clients cannot choose an arbitrary absolute path. A supplied `workspace` must match `projectName`.

### Set your DeepSeek key in YAML

Open `src/main/resources/application.yml` and replace `PASTE_YOUR_DEEPSEEK_API_KEY_HERE` with your DeepSeek API key. The file already sets `base-url: https://api.deepseek.com` and `model: deepseek-flash`. Spring AI uses its OpenAI-compatible client to call DeepSeek. Rebuild and restart the app after changing the YAML file.

## First request and approval loop

```powershell
$body = @{
  projectName = 'signup-page'
  prompt = 'Create a simple responsive HTML signup page with username and password fields. Both fields must not be empty or whitespace-only. Show validation errors when invalid and a success message when valid. Do not use a backend or database.'
  technology = @('HTML', 'CSS', 'JavaScript')
  approvalRequired = $true
} | ConvertTo-Json
$started = Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/projects/start' -ContentType 'application/json' -Body $body
$id = $started.workflowId
Invoke-RestMethod "http://localhost:8081/api/projects/$id/status"
Invoke-RestMethod "http://localhost:8081/api/projects/$id/files"
Invoke-RestMethod "http://localhost:8081/api/projects/$id/files/content?path=docs%2Frequirements.md"
```

Read `approvalId` and `stage` from the status response. Submit one decision at a time, then poll status for the next stage:

```powershell
$status = Invoke-RestMethod "http://localhost:8081/api/projects/$id/status"
$decision = @{ approvalId = $status.approvalId; stage = $status.stage; decision = 'APPROVE'; feedback = '' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/projects/$id/decision" -ContentType 'application/json' -Body $decision
Invoke-RestMethod "http://localhost:8081/api/projects/$id/status"
```

Repeat for `ARCHITECTURE`, `DEVELOPMENT`, `REVIEW`, and `QA`. To revise, use `decision = 'REVISE'` and provide nonempty `feedback`. To reject, use `decision = 'REJECT'`. Old approval IDs and wrong stages are rejected. Review the generated files under `$env:AI_WORKSPACE\signup-page`; open `index.html` in Chrome if the developer generated that file. The platform never assumes it will be present for every request.

More endpoints:

```powershell
Invoke-RestMethod "http://localhost:8081/api/projects/$id/history"
Invoke-RestMethod -Method Post "http://localhost:8081/api/projects/$id/cancel"
```

## cURL examples

```powershell
curl.exe -X POST http://localhost:8081/api/projects/start -H "Content-Type: application/json" --data-binary "@examples/signup-request.json"
curl.exe http://localhost:8081/api/projects/WORKFLOW_ID/status
curl.exe http://localhost:8081/api/projects/WORKFLOW_ID/files
curl.exe "http://localhost:8081/api/projects/WORKFLOW_ID/files/content?path=docs%2Frequirements.md"
curl.exe -X POST http://localhost:8081/api/projects/WORKFLOW_ID/decision -H "Content-Type: application/json" --data-binary "@examples/approve-requirements.json"
```

Replace `WORKFLOW_ID`, and use the current approval ID from `/status` in decision JSON. The example approval file is valid only for the first requirements revision.

Import `examples/postman-collection.json` into Postman. After starting a project, set the collection's `workflowId` variable from the response. For each decision, set `approvalId` and `stage` from `/status`.

## Generated project testing policy

Generated projects do **not** require a test suite or test execution. The QA Activity always creates `docs/qa-report.md`, inventories files, checks whether each file is nonempty, and asks the QA agent to describe test cases and coverage. Browser and Maven tests are marked **NOT RUN** unless a future isolated runner actually executes them. This policy applies to generated projects; the platform has its own unit tests.

## Persistence and safety

Temporal holds workflow state and durable approval waits. Project metadata is stored in the workspace root's `.platform` directory; each project has `.platform/status.json` and `.platform/history.json`. Documents and source are in the project folder. Restarting Spring Boot reconnects its worker to the same Temporal server. The Docker command above also persists the local Temporal development database in a Docker volume.

Project names are restricted to letters, digits, underscores, and hyphens. Workspace file paths must be relative and cannot contain traversal segments. Existing symlinks and junctions are rejected when paths are resolved. Generated file instructions are parsed and all paths validated before any file is written. Each file write uses a temporary file and atomic replacement. Concurrent creation of the same project directory fails.

API errors use JSON with `timestamp`, `status`, `error`, and `message`. The local development server and generated applications are for trusted local use. No generated source is executed by this platform.
