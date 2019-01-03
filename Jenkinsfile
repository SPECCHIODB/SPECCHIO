pipeline {
    agent any

    stages {
        stage('Preparation') {
            steps {
               git branch: 'master', url: 'https://github.com/SPECCHIODB/SPECCHIO.git'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew clean build'
                sh './gradlew izPackCreateInstaller'
            }
        }
        stage('Upload Archive') {
            steps {
                archiveArtifacts artifacts: 'src/client/build/distributions/*.zip', fingerprint: true
                archiveArtifacts artifacts: 'src/client/build/distributions/*.jar', fingerprint: true
                archiveArtifacts artifacts: 'src/webapp/build/libs/src/*.war', fingerprint: true
            }
        }
        stage('Build Javadoc') {
            steps {
                sh './gradlew aggregatedJavadocs'
            }
        }
        stage('Publish Javadoc') {
            steps {
                sh 'cp -r build/docs/. /var/www/javadoc/'
            }
        }
    }
}