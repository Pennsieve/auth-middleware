#!groovy

// Every build compiles and tests both libraries. A vX.Y.Z tag (pushed by
// release.sh) also publishes X.Y.Z of both to AWS CodeArtifact: the Scala
// library to `maven`, the Python package to `pypi`. No Jenkins credential:
// the agent's instance role gets the CodeArtifact token.

def codeArtifactToken = 'aws codeartifact get-authorization-token --region us-east-1 --domain pennsieve --domain-owner 932986170183 --query authorizationToken --output text'

node("executor") {
    checkout scm

    def releaseVersion = (env.TAG_NAME ==~ /^v\d+\.\d+\.\d+$/) ? env.TAG_NAME.substring(1) : null

    stage("Build") {
        parallel(
            "Python": {
                dir("python") {
                    sh "make build-release-container"
                }
            },

            "Scala": {
                dir("scala") {
                    sh "sbt clean compile"
                }
            }
        )
    }

    stage("Test") {
        parallel(
            "Python": {
                dir("python") {
                    sh "make ci-test"
                }
            },

            "Scala": {
                dir("scala") {
                    try {
                        sh "sbt test"
                    } finally {
                        junit '**/target/test-reports/*.xml'
                    }
                }
            }
        )
    }

    if (releaseVersion) {
        stage("Release ${releaseVersion}") {
            def fileVersion = readFile("VERSION").trim()
            if (fileVersion != releaseVersion) {
                error("Tag v${releaseVersion} doesn't match VERSION (${fileVersion})")
            }
            parallel(
                "Python": {
                    dir("python") {
                        // set +x: Jenkins traces sh commands, which would print the token.
                        sh "set +x; export TWINE_PASSWORD=\$(${codeArtifactToken}); VERSION=${releaseVersion} make ci-release"
                    }
                },

                "Scala": {
                    dir("scala") {
                        sh "sbt -Dversion=${releaseVersion} publish"
                    }
                }
            )
        }
    }
}
