// ============================================================
// Jenkinsfile — Node.js Pipeline
// Features covered for GitHub Actions Importer migration test:
//   ✅ Shared Libraries
//   ✅ Parallel Stages
//   ✅ Docker Build & Push
//   ✅ Environment Variables & Credentials
//   ✅ Manual Trigger & Push to main branch
//   ✅ Post Actions (Slack + Email Notifications)
//   ✅ Parameterized Builds
//   ✅ Stage-level timeouts
//   ✅ Agents & Docker agents
//   ✅ Stash/Unstash artifacts
// ============================================================

@Library('pipeline-shared-lib@main') _   // Shared Library import

// ---------- Parameters ----------
properties([
    parameters([
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'production'],
            description: 'Target deployment environment'
        ),
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test stage (use only in emergency)'
        ),
        string(
            name: 'DOCKER_TAG',
            defaultValue: 'latest',
            description: 'Docker image tag to build and push'
        )
    ]),

    // ---------- Triggers: Manual + Push to main ----------
    pipelineTriggers([
        // Trigger on any commit pushed to the main branch
        githubPush(),

        // Manual trigger via Jenkins UI or API (always available by default,
        // explicitly declared here for GitHub Actions Importer visibility)
        upstream(upstreamProjects: '', threshold: hudson.model.Result.SUCCESS)
    ])
])

// ---------- Global Environment Variables ----------
environment {
    APP_NAME          = 'jenkins-gha-demo'
    NODE_VERSION      = '18'
    DOCKER_REGISTRY   = 'docker.io/your-dockerhub-username'
    IMAGE_NAME        = "${DOCKER_REGISTRY}/${APP_NAME}"
    IMAGE_TAG         = "${params.DOCKER_TAG ?: env.BUILD_NUMBER}"
    DEPLOY_ENV        = "${params.DEPLOY_ENV}"

    // Credentials (stored in Jenkins Credentials Store)
    DOCKER_CREDS      = credentials('DOCKER_REGISTRY_CREDENTIALS')  // username:password
    SLACK_TOKEN       = credentials('SLACK_BOT_TOKEN')
    NPM_TOKEN         = credentials('NPM_TOKEN')
}

// ---------- Pipeline ----------
pipeline {

    // Default agent — run inside a Node.js Docker container
    agent {
        docker {
            image "node:${NODE_VERSION}-alpine"
            args  '-u root --privileged'
            reuseNode true
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        ansiColor('xterm')
    }

    stages {

        // --------------------------------------------------
        // STAGE 1 — Checkout & Setup
        // --------------------------------------------------
        stage('Checkout & Setup') {
            steps {
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build:  #${env.BUILD_NUMBER}"
                echo "Deploy Target: ${env.DEPLOY_ENV}"
                checkout scm

                // Inject NPM token for private packages
                sh '''
                    echo "//registry.npmjs.org/:_authToken=${NPM_TOKEN}" > .npmrc
                    node --version
                    npm --version
                '''
            }
        }

        // --------------------------------------------------
        // STAGE 2 — Install Dependencies
        // --------------------------------------------------
        stage('Install Dependencies') {
            steps {
                sh 'npm ci'
                // Stash node_modules for downstream stages
                stash name: 'node-modules', includes: 'node_modules/**'
            }
        }

        // --------------------------------------------------
        // STAGE 3 — Parallel: Lint + Unit Tests + Security Audit
        // --------------------------------------------------
        stage('Parallel: Quality Checks') {
            when {
                expression { return !params.SKIP_TESTS }
            }
            parallel {

                stage('Lint') {
                    steps {
                        unstash 'node-modules'
                        sh 'npm run lint || true'   // non-blocking lint
                    }
                }

                stage('Unit Tests') {
                    steps {
                        unstash 'node-modules'
                        sh 'npm test -- --ci --reporters=default --reporters=jest-junit'
                    }
                    post {
                        always {
                            junit 'junit.xml'
                            publishHTML([
                                reportDir  : 'coverage/lcov-report',
                                reportFiles: 'index.html',
                                reportName : 'Code Coverage Report'
                            ])
                        }
                    }
                }

                stage('Security Audit') {
                    steps {
                        unstash 'node-modules'
                        sh 'npm audit --audit-level=high || true'
                    }
                }

            } // end parallel
        }

        // --------------------------------------------------
        // STAGE 4 — Docker Build & Push (via Shared Library)
        // --------------------------------------------------
        stage('Docker Build & Push') {
            agent { label 'docker-agent' }  // Run on agent with Docker daemon
            environment {
                FULL_IMAGE = "${IMAGE_NAME}:${IMAGE_TAG}"
            }
            steps {
                unstash 'node-modules'
                script {
                    // Using shared library dockerUtils
                    dockerUtils.buildAndPush(APP_NAME, IMAGE_TAG, DOCKER_REGISTRY)
                    dockerUtils.scan(APP_NAME, IMAGE_TAG, DOCKER_REGISTRY)
                }
                echo "Docker image pushed: ${FULL_IMAGE}"
            }
        }

        // --------------------------------------------------
        // STAGE 5 — Deploy (environment-aware)
        // --------------------------------------------------
        stage('Deploy') {
            when {
                anyOf {
                    branch 'main'
                    branch 'release/*'
                }
            }
            steps {
                script {
                    if (env.DEPLOY_ENV == 'production') {
                        // Manual approval gate for production
                        timeout(time: 10, unit: 'MINUTES') {
                            input message: "Deploy to PRODUCTION?", ok: "Deploy"
                        }
                    }
                }
                sh '''
                    echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG} to ${DEPLOY_ENV}..."
                    # Replace with actual kubectl / helm / SSH deploy command
                    # Example: helm upgrade --install ${APP_NAME} ./helm --set image.tag=${IMAGE_TAG} --namespace=${DEPLOY_ENV}
                '''
            }
        }

    } // end stages

    // --------------------------------------------------
    // POST ACTIONS — Notifications (via Shared Library)
    // --------------------------------------------------
    post {
        success {
            script {
                notify.slack('SUCCESS', env.JOB_NAME)
            }
            archiveArtifacts artifacts: 'coverage/**', fingerprint: true
        }

        failure {
            script {
                notify.slack('FAILURE', env.JOB_NAME)
                notify.email('FAILURE', 'devops-team@your-org.com')
            }
        }

        unstable {
            script {
                notify.slack('UNSTABLE', env.JOB_NAME)
            }
        }

        aborted {
            script {
                notify.slack('ABORTED', env.JOB_NAME)
            }
        }

        always {
            cleanWs()  // Always clean the workspace
        }
    }

} // end pipeline