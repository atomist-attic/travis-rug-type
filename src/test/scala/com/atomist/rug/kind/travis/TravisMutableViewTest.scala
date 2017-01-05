package com.atomist.rug.kind.travis

import java.util

import com.atomist.source.StringFileArtifact
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.http.HttpHeaders

class TravisMutableViewTest extends FlatSpec with Matchers {

  val mockRepoKey: String = "-----BEGIN PUBLIC KEY-----\nMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA2uPPAb1DW15CqV0UEL0H\nV/2LUeb5riNdHLxMYIIxUXE3jwdHkLSiSeDRVeXdwG0rDHKpnA5QftnpjJjks24t\n8lL2gWbSYZMsFf1kf7TH0NcjTMsj3f+8fY1qkr1rWGrSr5CZ3j0g0FKbkAk1CtOq\nY94SvnFgJk4eumr4WxV20eNxQ13Po+2xHMDpOjKoXO6j7iBIatc06bNYR1m05nc7\nVzEO5QlmgE7NZjFHfYF9wl9bb+Z8/m0feiZaEB0xyKpBoy9Gf60odi5fUQnEp1S9\nRjshAN3OsfjdXoFlMCQCmCihayoZ6ktWN0MLhb1+xPosL6RnsIHphm01/Tk1Q6FG\nZp/3mx4fAJjG3yd5o86BZsW24MQYF2uUsOxa62yRdLxljG7Hon7GFzcRsFlhqteI\nF44PV7XVJAXDDX2esJcBFii5/MAwt019O0rrUGU3tkEG9cRkQbGjN6PRubmcKPq2\nJLwa818CcNWA9vD5aju6XX7jBBxmYWzWEORw8YXk6m1VZg/Tyh+3MCA0F5uViwk5\nYefvTHj7YK0lM6ZAdTcEO9B1JSAsbqhQDK26LlPZ7220uFvJqxsinUuAWWWu7JiD\njYqBDhobz/fB/dnLtWM2euztX+4eJGBQ90236MNUTKIPW2En5LDILwsRcMQjct9N\nTVuQ0Ko1nlpaK8sEEBuo9BMCAwEAAQ==\n-----END PUBLIC KEY-----\n"

  class MockTravisEndpoints extends TravisEndpoints {

    def getRepoKey(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): String = mockRepoKey

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

  "ExportedFunctions" should "be able to be called" in {
    val repo: String = "dummy/repo"
    val githubToken: String = "abcdef0123456789abcdef0123456789abcdef01"
    val org: String = ".org"

    tmv.enable(repo, githubToken, org)
    tmv.encrypt(repo, githubToken, org, "VARIABLE=value")
    tmv.disable(repo, githubToken, org)
  }
}
