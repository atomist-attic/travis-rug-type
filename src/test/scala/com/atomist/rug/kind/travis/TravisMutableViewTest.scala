package com.atomist.rug.kind.travis

import java.util

import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.http.HttpHeaders

class TravisMutableViewTest extends FlatSpec with Matchers {

  class MockTravisEndpoints extends TravisEndpoints {

    def getRepoKey(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): String = "notarealykey"

    def putHook(endpoint: TravisAPIEndpoint, headers: HttpHeaders, body: util.HashMap[String, Object]): Unit = Unit

    def postUsersSync(endpoint: TravisAPIEndpoint, headers: HttpHeaders): Unit = Unit

    def getRepo(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): Int = 8675309

    def postAuthGitHub(endpoint: TravisAPIEndpoint, githubToken: String): String = "xZ-dkkuUBH7823CMfm3WeR"

  }

  val f = StringFileArtifact(
    ".travis.yml",
    """
      |install: true
      |script: true
    """.stripMargin
  )
  val tmv = new TravisMutableView(f, null, new MockTravisEndpoints)

}
