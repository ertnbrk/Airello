pipeline {
    agent any

    environment {
        AWS_REGION = 'eu-central-1'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        ECR_REPOSITORY = 'planmate-api'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build & Test') {
            steps {
                sh './gradlew clean build --no-daemon'
                junit 'build/test-results/test/*.xml'
                jacoco execPattern: 'build/jacoco/test.exec'
            }
        }

        stage('Code Quality') {
            steps {
                sh './gradlew checkstyleMain checkstyleTest spotlessCheck'
            }
        }

        stage('Dockerize') {
            steps {
                script {
                    docker.build("${ECR_REPOSITORY}:${IMAGE_TAG}")
                }
            }
        }

        stage('Push to ECR') {
            steps {
                script {
                    sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    docker.image("${ECR_REPOSITORY}:${IMAGE_TAG}").push()
                    docker.image("${ECR_REPOSITORY}:${IMAGE_TAG}").push('latest')
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                sh """
                    helm upgrade --install planmate-api ./helm/planmate-api \\
                      --namespace staging \\
                      --set image.tag=${IMAGE_TAG} \\
                      --set environment=staging \\
                      --wait
                """
            }
        }

        stage('Smoke Tests') {
            when {
                branch 'main'
            }
            steps {
                sh 'curl -f http://planmate-api.staging/actuator/health || exit 1'
            }
        }

        stage('Promote to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                sh """
                    helm upgrade --install planmate-api ./helm/planmate-api \\
                      --namespace production \\
                      --set image.tag=${IMAGE_TAG} \\
                      --set environment=prod \\
                      --wait
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
