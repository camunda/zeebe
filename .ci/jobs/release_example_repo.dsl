pipelineJob('camunda-optimize-example-repo-release') {

  displayName 'Release Camunda Optimize Example Repository'
  description 'Release Camunda Optimize Example Repository, add a tag and amend the version list in readme.'

  // By default, this job is disabled in non-prod envs.
  if (binding.variables.get("ENVIRONMENT") != "prod") {
    disabled()
  }

  definition {
    cpsScm {
      scriptPath('.ci/pipelines/release_example_repo.groovy')
      lightweight(false)
      scm {
        git {
          remote {
            github('camunda/camunda-optimize')
            credentials('camunda-jenkins-github')
          }
          branches('master')
        }
      }
    }
  }

  parameters {
    stringParam('RELEASE_VERSION', '2.3.0', 'Version to release. Applied to pom.xml, Git tag and readme overview.')
    stringParam('DEVELOPMENT_VERSION', '2.4.0-SNAPSHOT', 'Next development version.')
    stringParam('BRANCH', 'master', 'The branch used for the release checkout.')
  }

}
