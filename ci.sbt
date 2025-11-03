import _root_.io.github.nafg.mergify.dsl._
import sbtdynver.GitDirtySuffix


mergifyExtraConditions := Seq(
  (Attr.Author :== "scala-steward") ||
    (Attr.Author :== "nafg-scala-steward[bot]")
)

inThisBuild(List(
  homepage                            := Some(url("https://github.com/nafg/scalajs-facades")),
  licenses                            := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  developers                          := List(
    Developer("nafg", "Naftoli Gugenheim", "98384+nafg@users.noreply.github.com", url("https://github.com/nafg"))
  ),
  dynverGitDescribeOutput ~= (_.map(o => o.copy(dirtySuffix = GitDirtySuffix("")))),
  dynverSonatypeSnapshots             := true,
  versionScheme                       := Some("early-semver"),
  githubWorkflowScalaVersions         := githubWorkflowScalaVersions.value.map(_.replaceFirst("\\d+$", "x")),
  githubWorkflowJobSetup +=
    WorkflowStep.Use(UseRef.Public("actions", "setup-node", "v4"), params = Map("node-version" -> "20")),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish               := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  )
))
