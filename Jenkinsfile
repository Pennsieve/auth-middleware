#!groovy

node("executor") {
    checkout scm

    def pennsieveNexusCreds = usernamePassword(
        credentialsId: "pennsieve-nexus-ci-login",
        usernameVariable: "PENNSIEVE_NEXUS_USER",
        passwordVariable: "PENNSIEVE_NEXUS_PW"
    )

    stage("Build") {
        parallel(
            "Python": {
                dir("python") {
                    sh "make build-release-container"
                }
            },

            "Scala": {
                dir("scala") {
                    withCredentials([pennsieveNexusCreds]) {
                        sh "sbt clean compile"
                    }
                }
            }
        )
    }

    stage("Test") {
        parallel(
            "Python": {
                dir("python") {
                    withCredentials([pennsieveNexusCreds]) {
                        sh "make ci-test"
                    }
                }
            },

            "Scala": {
                dir("scala") {
                    withCredentials([pennsieveNexusCreds]) {
                        try {
                            sh "sbt test"
                        } finally {
                            junit '**/target/test-reports/*.xml'
                        }
                    }
                }
            }
        )
    }
}
