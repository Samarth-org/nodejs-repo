#!/usr/bin/env groovy

/**
 * Shared Library: notify.groovy
 * Provides reusable notification utilities for pipelines.
 * Usage: notify.slack('SUCCESS', 'my-pipeline')
 */

def slack(String status, String pipelineName) {
    def colorMap = [
        'SUCCESS' : '#36a64f',
        'FAILURE' : '#d9534f',
        'UNSTABLE': '#f0ad4e',
        'ABORTED' : '#808080'
    ]
    def color = colorMap.get(status, '#808080')
    def message = """
        *Pipeline:* ${pipelineName}
        *Status:*   ${status}
        *Branch:*   ${env.BRANCH_NAME ?: 'N/A'}
        *Build:*    <${env.BUILD_URL}|#${env.BUILD_NUMBER}>
        *Duration:* ${currentBuild.durationString}
    """.stripIndent()

    slackSend(color: color, message: message)
}

def email(String status, String recipients) {
    emailext(
        subject: "[Jenkins] ${status}: Job '${env.JOB_NAME}' [${env.BUILD_NUMBER}]",
        body: """
            <p>Build <b>${status}</b></p>
            <p>Job: ${env.JOB_NAME}</p>
            <p>Build Number: ${env.BUILD_NUMBER}</p>
            <p>Branch: ${env.BRANCH_NAME ?: 'N/A'}</p>
            <p>Check console output at: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
        """,
        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
        to: recipients
    )
}

return this