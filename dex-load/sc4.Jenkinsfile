pipeline {
    agent {
        label 'buildagent-matcher'
    }
    options {
        ansiColor('xterm')
    }
    parameters {
        string(name: 'SBT_THREAD_NUMBER', defaultValue: '6', description: '')
        string(name: 'SEED', defaultValue: 'test-seed', description: '')
        string(name: 'AN', defaultValue: '60', description: '')
        string(name: 'RC', defaultValue: '10000', description: '')
        string(name: 'RT', defaultValue: '6', description: '')
        string(name: 'AS', defaultValue: 'D', description: '')
    }
    environment {
        SBT_HOME = tool name: 'sbt-1.2.6', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
        SBT_THREAD_NUMBER = "${SBT_THREAD_NUMBER}"
        SBT_OPTS = '-Xmx10g -XX:ReservedCodeCacheSize=128m -XX:+CMSClassUnloadingEnabled'
        PATH = "${env.SBT_HOME}/bin:${env.PATH}"
        SEED = "${SEED}"
        AN = "${AN}"
        RC = "${RC}"
        RT = "${RT}"
        AS = "${AS}"
        NODE = "${NODE}"
        MATCHER = "${MATCHER}"
        AIM = "${AIM}"
    }
    stages {
        stage('Cleanup') {
            steps {
                sh 'rm -rf ./dex-load/*.txt'
                sh 'git fetch --tags'
                sh 'find ~/.sbt/1.0/staging/*/waves -type d -name target | xargs -I{} rm -rf {}'
                sh 'find . -type d -name target | xargs -I{} rm -rf {}'
                sh 'sbt "cleanAll"'
            }
        }
        stage('Generate ammo file') {
            steps {
                sh 'sbt "project dex-load" generateAmmo'
            }
        }
        stage('Generate feeder file') {
            steps {
                sshagent (credentials: ['buildagent-matcher']) {
                    sh "scp buildagent-matcher@${LOADGEN}:/home/buildagent-matcher/key.txt ./dex-load"
                    sh "scp buildagent-matcher@${LOADGEN}:/home/buildagent-matcher/pairs.txt ./dex-load"
                }
                sh 'sbt "project dex-load" generateFeeder'
            }
        }
        stage('Performance Test') {
            parallel {
                 stage("REST") {
                    steps {
                        sshagent (credentials: ['buildagent-matcher']) {
                             sh "ssh -o StrictHostKeyChecking=no -l buildagent-matcher ${LOADGEN} hostname"

                             sh "scp ./dex-load/requests-*.txt buildagent-matcher@${LOADGEN}:/home/buildagent-matcher"
                             sh "scp ./dex-load/src/main/resources/runLoadTest.sh buildagent-matcher@${LOADGEN}:/home/buildagent-matcher"
                             sh "ssh -q buildagent-matcher@${LOADGEN} sudo sh runLoadTest.sh"
                             script {
                                OVERLOAD = sh(script:"ssh -q buildagent-matcher@${LOADGEN} ls /home/yatank/loadtest/logs/lunapark", returnStdout: true)
                                GRAFANA = sh( script: '''
                                                        echo "https://${GRAFANA_URL}/d/WsyjIiHiz/system-metrics?orgId=5&var-hostname=${MATCHER_URL}&from=$(date -d '- 20 minutes' +'%s')000&to=$(date -d '+ 5 minutes' +'%s')000"
                                                      ''', returnStdout: true)
                                currentBuild.description = "<a href='https://overload.yandex.net/${OVERLOAD}'>Yandex</a> <br/> <a href='${GRAFANA}'>Grafana</a>"
                             }
                        }
                    }
                 }
                 stage("Web Socket") {
                    steps {
                        sleep time: 1, unit: 'MINUTES'
                        sh 'mv ./dex-load/feeder.csv ./dex-ws-load/'
                        sh "cd ./dex-ws-load && sbt gatling:testOnly load.ConnectionsAndStreamsTest -Dff=feeder.csv -Dws=${WS_URL} -Drt=30"
                        script {
                            GRAFANA = sh(script: '''
                                                    echo "https://${GRAFANA_URL}/d/WsyjIiHiz/system-metrics?orgId=5&var-hostname=${MATCHER_URL}&from=$(date -d '- 20 minutes' +'%s')000&to=$(date -d '+ 5 minutes' +'%s')000"
                                                 ''', returnStdout: true)
                            currentBuild.description = "<a href='${GRAFANA}'>Grafana</a>"
                        }
                    }
                 }
            }
        }
    }
}