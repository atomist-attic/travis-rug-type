package com.atomist.rug.kind.travis

import java.net.URI
import java.util
import java.util.Collections

import com.atomist.rug.InvalidRugParameterPatternException
import org.springframework.http.{HttpHeaders, HttpMethod, HttpStatus, RequestEntity}
import org.springframework.web.client.{HttpClientErrorException, RestClientException, RestTemplate}

trait TravisAPIEndpoint {

  def tld: String

}

object TravisAPIEndpoint {

  def stringToTravisEndpoint(ep: String): TravisAPIEndpoint = ep match {
    case ".org" | "org" => TravisOrgEndpoint
    case ".com" | "com" => TravisComEndpoint
    case _ => throw new InvalidRugParameterPatternException("Travis CI endpoint must be 'org' or 'com'")
  }

}

object TravisOrgEndpoint extends TravisAPIEndpoint {
  val tld: String = "org"
}

object TravisComEndpoint extends TravisAPIEndpoint {
  val tld: String = "com"
}

trait TravisEndpoints {
  /** Return the key of the repo.
    *
    * @param endpoint org|com
    * @param headers standard Travis API headers
    * @param repo repo slug, e.g., "owner/name"
    * @return Repo key
    */
  def getRepoKey(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): String

  /** Enable or disable CI for the repo.
    *
    * @param endpoint org|com
    * @param headers standard Travis API headers
    * @param body hook body see Travis API docs
    */
  def putHook(endpoint: TravisAPIEndpoint, headers: HttpHeaders, body: util.HashMap[String, Object]): Unit

  /** Refresh the list of repos available to user in Travis CI.
    *
    * @param endpoint org|com
    * @param headers standard Travis API headers
    */
  def postUsersSync(endpoint: TravisAPIEndpoint, headers: HttpHeaders): Unit

  /** Get repo unique integer identifier.
    *
    * @param endpoint org|com
    * @param headers standard Travis API headers
    * @param repo repo slug, e.g., "owner/name"
    * @return unique repo identifier
    */
  def getRepo(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): Int

  /** Authenticate with Travis using GitHub token.
    *
    * Careful, these do not seem to expire.
    *
    * @param endpoint org|com
    * @param githubToken GitHub personal access token with appropriate scope for endpoint
    * @return Travis token
    */
  def postAuthGitHub(endpoint: TravisAPIEndpoint, githubToken: String): String
}

object TravisEndpoints {

  /** Construct standard Travis API headers.
    *
    * @param endpoint org|com
    * @return Travis API headers
    */
  def headers(endpoint: TravisAPIEndpoint): HttpHeaders = {
    val noAuthHeaders = new HttpHeaders()
    noAuthHeaders.add("Content-Type", "application/json")
    noAuthHeaders.add("Accept", "application/vnd.travis-ci.2+json")
    noAuthHeaders.add("User-Agent", "Travis/1.6.8")
    noAuthHeaders
  }

  /** Construct standard Travis API headers with Authorization token.
    *
    * @param endpoint org|com
    * @param token Travis API token returned from TravisEndpoints.postAuthGitHub
    * @return Travis API headers
    */
  def authHeaders(endpoint: TravisAPIEndpoint, token: String): HttpHeaders = {
    val hdrs = headers(endpoint)
    hdrs.add("Authorization", s"""token "$token"""")
    hdrs
  }
}

class RealTravisEndpoints extends TravisEndpoints {

  private val restTemplate: RestTemplate = new RestTemplate()

  def getRepoKey(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): String = {
    val request = new RequestEntity[util.Map[String, Object]](
      headers,
      HttpMethod.GET,
      URI.create(s"https://api.travis-ci.${endpoint.tld}/repos/$repo/key")
    )
    val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]])
    responseEntity.getBody.get("key").asInstanceOf[String]
  }

  def putHook(endpoint: TravisAPIEndpoint, headers: HttpHeaders, body: util.HashMap[String, Object]): Unit = {
    val url: String = s"https://api.travis-ci.${endpoint.tld}/hooks"
    // why are the URL and HTTP action specified twice?
    val request = new RequestEntity(
      body,
      headers,
      HttpMethod.PUT,
      URI.create(url)
    )
    restTemplate.put(url, request)
  }

  def postUsersSync(endpoint: TravisAPIEndpoint, headers: HttpHeaders): Unit = {
    val request = new RequestEntity[util.Map[String, Object]](
      headers,
      HttpMethod.POST,
      URI.create(s"https://api.travis-ci.${endpoint.tld}/users/sync")
    )
    try {
      restTemplate.exchange(request, classOf[util.Map[String, Object]])
    }
    catch {
      case he: HttpClientErrorException if he.getStatusCode == HttpStatus.CONFLICT =>
    }
    import scala.util.control.Breaks._
    breakable {
      for (i <- 0 to 30) {
        try {
          val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]])
          if (responseEntity.getStatusCode == HttpStatus.OK) {
            break
          }
        }
        catch {
          case he: HttpClientErrorException if he.getStatusCode == HttpStatus.CONFLICT =>
            print(s"  Waiting for repositories to sync ($i)")
            Thread.sleep(1000L)
          case _: Throwable => break
        }
      }
    }
  }

  def getRepo(endpoint: TravisAPIEndpoint, headers: HttpHeaders, repo: String): Int = {
    val request = new RequestEntity[util.Map[String, Object]](
      headers,
      HttpMethod.GET,
      URI.create(s"https://api.travis-ci.${endpoint.tld}/repos/$repo")
    )
    val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]])
    val repoObject: util.Map[String, Object] = responseEntity.getBody.get("repo").asInstanceOf[util.Map[String, Object]]
    repoObject.get("id").asInstanceOf[Int]
  }

  // Use evil var to cache token because Travis CI does not want you to
  // repeatedly get new tokens.
  private[travis] var travisToken = ""
  def postAuthGitHub(endpoint: TravisAPIEndpoint, githubToken: String): String =
    if (travisToken != "") travisToken
    else {
      val request = new RequestEntity(
        Collections.singletonMap("github_token", githubToken),
        TravisEndpoints.headers(endpoint),
        HttpMethod.POST,
        URI.create(s"https://api.travis-ci.${endpoint.tld}/auth/github")
      )
      val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, String]])
      travisToken = responseEntity.getBody.get("access_token")
      travisToken
    }

}
