pipeline {
    // 执行脚本的jenkins节点
    agent any

    tools {
            maven 'docker_maven'
    }

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Git branch to build')
    }

    environment {
        GIT_HTTP_VERSION=1.1
        DOCKER_REPO = 'feishuservice:1.0'
    }

    stages {
        stage('拉取代码') {
            steps {
                script {
                    git branch: "${params.BRANCH_NAME}", credentialsId: "f13d0744-5c8c-4bb5-8da8-074742ec82d1", url: 'https://1322969163-dd5vjgrlqh.na-siliconvalley.tencentscf.com/wankaiyi/feishuservice.git'
                }
            }
        }

        stage('Maven打包') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('构建镜像') {
            steps {
                withCredentials([
                    string(credentialsId: '9757f236-8f97-4699-815b-7d7744f78ad3', variable: 'APOLLO_ACCESS_KEY'),
                    string(credentialsId: 'cf6b4bbc-c202-4f79-b57e-9e4bda925870', variable: 'HOST')
                    ]) {
                    script {
                         sh """
                                docker build \
                                --build-arg APOLLO_ACCESS_KEY=${APOLLO_ACCESS_KEY} \
                                --build-arg HOST=${HOST} \
                                -t ${DOCKER_REPO} .
                            """
                    }
                }
            }
        }

        stage('推送镜像') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: '6cbdcba1-1a00-481b-8d98-f89cfb060b2a', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login 43.139.29.48:9999 -u $DOCKER_USERNAME --password-stdin'
                        sh 'docker push 43.139.29.48:9999/feishuservice/feishuservice:1.0'
                    }
                }
            }
        }

        stage('启动容器') {
            steps {
                script {
                  sh '''
                     docker ps -q --filter "name=feishuservice" | grep -q . && docker stop feishuservice || true
                     docker ps -a -q --filter "name=feishuservice" | grep -q . && docker rm feishuservice || true
                     docker run -d --name feishuservice -p 9090:9090 feishuservice:1.0
                  '''
                }
            }

        }
    }
}