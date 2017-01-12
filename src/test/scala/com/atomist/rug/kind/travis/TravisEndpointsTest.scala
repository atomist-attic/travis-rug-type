package com.atomist.rug.kind.travis

import java.util.Collections

import com.atomist.rug.InvalidRugParameterPatternException
import org.scalatest.{FlatSpec, Matchers}

class TravisEndpointsTest extends FlatSpec with Matchers {

  import TravisAPIEndpoint._

  "stringToTravisEndpoint" should "accept org" in {
    stringToTravisEndpoint("org") should be(TravisOrgEndpoint)
  }

  it should "accept .org" in {
    stringToTravisEndpoint(".org") should be(TravisOrgEndpoint)
  }

  it should "accept com" in {
    stringToTravisEndpoint("com") should be(TravisComEndpoint)
  }

  it should "accept .com" in {
    stringToTravisEndpoint(".com") should be(TravisComEndpoint)
  }

  it should "throw an exception if not given a valid API type" in {
    an[InvalidRugParameterPatternException] should be thrownBy stringToTravisEndpoint(".blah")
  }

  "RealTravisEndpoints" should "return cached token" in {
    val t: String = "notarealtravistoken"
    val rte: RealTravisEndpoints = new RealTravisEndpoints
    rte.travisTokens = Collections.singletonMap("doesnotmatter", "notarealtravistoken")
    val api: TravisAPIEndpoint = TravisOrgEndpoint
    rte.postAuthGitHub(api, "doesnotmatter") should be(t)
  }

}
