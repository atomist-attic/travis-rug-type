package com.atomist.rug.kind.travis

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource
import com.atomist.source.file.ClassPathArtifactSource
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class TravisTypeMutableViewTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  var uut: TravisTypeMutableView = _

  val testProject = ClassPathArtifactSource.toArtifactSource("./travisproject")

  lazy val customTypeFile = testProject.findFile("my.travis").get

  override def beforeEach() {
    uut = new TravisTypeMutableView(customTypeFile, new ProjectMutableView(EmptyArtifactSource(""), testProject))
  }

  "TravisTypeMutableView" should "get something useful and not mutate anything" in {
    val expectedResponse = "myvalue"
    uut.customProperty should be (expectedResponse)
    uut.dirty should be (false)
  }
}