#!/usr/bin/env groovy

/**
 * Shared Library: dockerUtils.groovy
 * Reusable Docker build/push/scan utilities.
 * Usage: dockerUtils.buildAndPush('my-image', '1.0.0', 'my-registry')
 */

def buildAndPush(String imageName, String imageTag, String registry) {
    def fullImage = "${registry}/${imageName}:${imageTag}"

    echo "Building Docker image: ${fullImage}"
    sh "docker build -f docker/Dockerfile -t ${fullImage} ."

    echo "Pushing Docker image: ${fullImage}"
    docker.withRegistry("https://${registry}", 'DOCKER_REGISTRY_CREDENTIALS') {
        docker.image(fullImage).push()
        docker.image(fullImage).push('latest')
    }
}

def scan(String imageName, String imageTag, String registry) {
    def fullImage = "${registry}/${imageName}:${imageTag}"
    echo "Scanning image for vulnerabilities: ${fullImage}"
    // Trivy or Snyk scan placeholder
    sh "echo 'Scanning ${fullImage} ... (integrate Trivy/Snyk here)'"
}

return this