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
        stage('Build Javadoc') {
            steps {
                sh './gradlew bundleJavadocs'
            }
        }
        stage('Upload Archive') {
            steps {
                archiveArtifacts artifacts: 'src/client/build/distributions/specchio-installer.jar', fingerprint: true
                archiveArtifacts artifacts: 'src/client/build/distributions/specchio-client.zip', fingerprint: true
                archiveArtifacts artifacts: 'src/webapp/build/distributions/specchio-webapp.zip', fingerprint: true
                archiveArtifacts artifacts: 'src/MacOS_Installer/build/distributions/specchio-installer_MacOS.jar', fingerprint: true
                archiveArtifacts artifacts: 'build/docs/javadoc.zip', fingerprint: true
            }
        }
        stage('Publish Javadoc') {
            steps {
                sh 'cp -r build/docs/. /var/www/javadoc/'
            }
        }
    }
}