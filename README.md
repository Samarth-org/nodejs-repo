# Jenkins → GitHub Actions Migration Demo

A feature-rich Node.js pipeline repo designed to **stress-test the GitHub Actions Importer** tool.

---

## 📁 Repository Structure

```
├── Jenkinsfile                   # Main pipeline (feature-rich)
├── package.json                  # Node.js project manifest
├── .eslintrc.json                # ESLint config
├── .gitignore
├── src/
│   └── app.js                    # Express.js app
├── test/
│   └── app.test.js               # Jest unit tests
├── docker/
│   ├── Dockerfile                # Multi-stage Docker build
│   └── docker-compose.yml        # Local dev setup
├── vars/                         # Jenkins Shared Library vars
│   ├── notify.groovy             # Slack + Email notifications
│   └── dockerUtils.groovy        # Docker build/push/scan helpers
└── resources/
    └── library-config.yml        # Shared library documentation
```

---

## ✅ Jenkins Features Covered (for migration validation)

| Feature | Jenkinsfile Location |
|---|---|
| Shared Libraries (`@Library`) | Top of Jenkinsfile |
| Parameterized Builds | `properties([parameters([...])])` |
| Scheduled Trigger (cron) | `pipelineTriggers([cron(...)])` |
| SCM Polling | `pipelineTriggers([pollSCM(...)])` |
| Environment Variables | `environment { }` block |
| Jenkins Credentials | `credentials('...')` binding |
| Docker Agent | `agent { docker { image '...' } }` |
| Parallel Stages | Stage: `Parallel: Quality Checks` |
| Stash / Unstash | After `npm ci`, reused in parallel |
| `when` conditions (branch/param) | Multiple stages |
| SonarQube + Quality Gate | Stage: `SonarQube Analysis` |
| Docker Build & Push (shared lib) | Stage: `Docker Build & Push` |
| Manual Approval (input) | Stage: `Deploy` (production only) |
| Post Actions (success/failure) | `post { }` block |
| Slack Notifications (shared lib) | `notify.slack(...)` |
| Email Notifications (shared lib) | `notify.email(...)` |
| Archive Artifacts | `archiveArtifacts` in post |
| JUnit Test Results | `junit 'junit.xml'` |
| HTML Coverage Report | `publishHTML(...)` |
| Workspace Cleanup | `cleanWs()` in `post.always` |
| Timeouts (pipeline + stage) | `options { timeout(...) }` |
| Build Discarder | `logRotator(...)` |

---

## 🚀 How to Use with GitHub Actions Importer

### 1. Install the Importer
```bash
gh extension install github/gh-actions-importer
```

### 2. Configure credentials
```bash
gh actions-importer configure
```

### 3. Audit your Jenkins instance
```bash
gh actions-importer audit jenkins \
  --source-url https://your-jenkins-url \
  --output-dir ./audit-output
```

### 4. Dry-run migration for this pipeline
```bash
gh actions-importer dry-run jenkins \
  --source-url https://your-jenkins-url/job/your-pipeline \
  --output-dir ./output
```

### 5. Migrate
```bash
gh actions-importer migrate jenkins \
  --source-url https://your-jenkins-url/job/your-pipeline \
  --target-url https://github.com/your-org/your-repo \
  --output-dir ./output
```

---

## 🔐 Required Jenkins Credentials (for testing)

| Credential ID | Type | Used For |
|---|---|---|
| `DOCKER_REGISTRY_CREDENTIALS` | Username/Password | Docker push |
| `SLACK_BOT_TOKEN` | Secret Text | Slack notifications |
| `SONAR_TOKEN` | Secret Text | SonarCloud analysis |
| `NPM_TOKEN` | Secret Text | Private npm packages |

---

## 🐳 Run Locally

```bash
npm install
npm test
docker compose -f docker/docker-compose.yml up --build
```