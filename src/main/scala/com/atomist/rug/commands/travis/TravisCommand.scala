package com.atomist.rug.commands.travis

import com.atomist.rug.kind.service.ServicesMutableView
import com.atomist.rug.kind.travis.{RealTravisEndpoints, TravisAPIEndpoint, TravisEndpoints}
import com.atomist.rug.spi.Command
import org.springframework.http.HttpHeaders

class TravisCommand extends Command[ServicesMutableView] {

  override def nodeTypes: Set[String] = Set("services")

  override def name: String = "travis"

  override def invokeOn(treeNode: ServicesMutableView): AnyRef = {
    new TravisOperations(new RealTravisEndpoints)
  }
}

class TravisOperations(travisEndpoints: TravisEndpoints) {

  def restartBuild(buildId: Int, org: String, token: String): TravisStatus = {
    val api: TravisAPIEndpoint = TravisAPIEndpoint.stringToTravisEndpoint(org)
    val travisToken: String = travisEndpoints.postAuthGitHub(api, token)
    val headers: HttpHeaders = TravisEndpoints.authHeaders(api, travisToken)
    try {
      travisEndpoints.postRestartBuild(api, headers, buildId)
      new TravisStatus(true, s"Successfully restarted build `${buildId}` on Travis CI")
    }
    catch {
      case e: Exception => new TravisStatus(false, e.getMessage)
    }
  }
}

case class TravisStatus(success: Boolean = true, message: String = "")
