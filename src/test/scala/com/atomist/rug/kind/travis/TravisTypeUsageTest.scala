package com.atomist.rug.kind.travis

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.NoModificationNeeded
import com.atomist.source.EmptyArtifactSource
import com.atomist.source.file.ClassPathArtifactSource
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

class TravisTypeUsageTest extends FlatSpec with Matchers with LazyLogging {

  import com.atomist.rug.TestUtils._

  val testProject = ClassPathArtifactSource.toArtifactSource("./travisproject")

  "TravisType" should "access something with native Rug function and no change" in {
    val prog =
      """
        |editor TravisTypeUsageEditor
        |
        |with travis when path = "my.travis"
        | do customProperty
      """.stripMargin

    val filename = "thing.yml"

    val newName = "Foo"
    attemptModification(prog, testProject, EmptyArtifactSource(""), SimpleProjectOperationArguments("", Map(
      "new_name" -> newName
    ))) match {
      case nmn: NoModificationNeeded =>
    }
  }
}