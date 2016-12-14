package com.atomist.rug.kind.travis

import java.io.StringReader
import java.net.URI
import java.security.{KeyFactory, Security}
import java.security.spec.X509EncodedKeySpec
import java.util
import java.util.{Base64, Collections}
import javax.crypto.Cipher

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.springframework.http.{HttpHeaders, HttpMethod, HttpStatus, RequestEntity}
import org.springframework.web.client.{HttpClientErrorException, RestTemplate}
import org.yaml.snakeyaml.{DumperOptions, Yaml}

object TravisTypeMutableView {
  val options = new DumperOptions()
  options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
  options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN)
  val yaml = new Yaml(options)

  Security.addProvider(new BouncyCastleProvider)
}

class TravisTypeMutableView(
                             originalBackingObject: FileArtifact,
                             parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  override def nodeName = "travis"

  override def nodeType = "travis"

  import TravisTypeMutableView._

  private val mutatableContent = (yaml.load(originalBackingObject.content)).asInstanceOf[util.Map[String, Any]]

  override protected def currentContent: String = yaml.dump(mutatableContent).replace("'","")

  private val restTemplate: RestTemplate = new RestTemplate()

  @ExportFunction(readOnly = false, description = "Enables a project for Travis CI")
  def encrypt(@ExportFunctionParameterDescription(name = "repo",
    description = "Repo slug") repo: String, @ExportFunctionParameterDescription(name = "github_token",
    description = "GitHub Token") token: String, @ExportFunctionParameterDescription(name = "org",
    description = ".com or .org") org: String, @ExportFunctionParameterDescription(name = "content",
    description = "Content") content: String): Unit = {

    val authHeaders = getAuth(token, org)

    val request = new RequestEntity[util.Map[String, Object]](authHeaders, HttpMethod.GET,
      URI.create(s"https://api.travis-ci${org}/repos/${repo}/key"));
    val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]]);
    val key = responseEntity.getBody.get("key").asInstanceOf[String]

    val parser = new PEMParser(new StringReader(key))
    val ob = parser.readObject().asInstanceOf[SubjectPublicKeyInfo]

    val pubKeySpec = new X509EncodedKeySpec(ob.getEncoded())
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKey = keyFactory.generatePublic(pubKeySpec)

    val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)

    val secured = s"secure: ${Base64.getEncoder().encodeToString(rsaCipher.doFinal(content.getBytes()))}"

    if (mutatableContent.containsKey("env") && mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].containsKey("global")) {
      if (mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].get("global").asInstanceOf[util.List[String]] != null) {
        val global = mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].get("global").asInstanceOf[util.List[String]]
        global.add(secured)
      }
      else {
        val global: util.List[String] = new util.ArrayList[String]()
        mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].put("global", global);
        global.add(secured)
      }
    }
    else {
      val env: util.Map[String, Any] = new util.HashMap[String, Any]()
      mutatableContent.put("env", env)
      val global: util.List[String] = new util.ArrayList[String]()
      env.put("global", global);
      global.add(secured)
    }

    content match {
      case s: String if content.contains("=") => print(s"  Added encrypted value for ${content.substring(0, content.indexOf("="))}")
      case _ => print("  Added encrypted value")
    }

  }

  @ExportFunction(readOnly = true, description = "Enables a project for Travis CI")
  def enable(@ExportFunctionParameterDescription(name = "repo",
    description = "Repo slug") repo: String, @ExportFunctionParameterDescription(name = "github_token",
    description = "GitHub Token") token: String, @ExportFunctionParameterDescription(name = "org",
    description = ".com or .org") org: String): Unit = {
    hook(true, repo, token, org)
    print(s"  Enabled repository" )
  }

  @ExportFunction(readOnly = true, description = "Disables a project for Travis CI")
  def disable(@ExportFunctionParameterDescription(name = "repo",
    description = "Repo slug") repo: String, @ExportFunctionParameterDescription(name = "github_token",
    description = "GitHub Token") token: String, @ExportFunctionParameterDescription(name = "org",
    description = ".com or .org") org: String): Unit = {
    hook(false, repo, token, org)
    print(s"  Disabled repository" )
  }

  def hook(active: Boolean, repo: String, token: String, org: String) = {
    val authHeaders = getAuth(token, org)
    sync(authHeaders, org)
    val id: Int = getRepo(authHeaders, repo, org)

    val hook = new util.HashMap[String, Any]()
    hook.put("id", id)
    hook.put("active", true)
    val body = new util.HashMap[String, Object]()
    body.put("hook", hook)

    val request = new RequestEntity(body, getAuth(token, org), HttpMethod.PUT, URI.create(s"https://api.travis-ci${org}/hooks"));
    restTemplate.put(s"https://api.travis-ci${org}/hooks", request);
  }

  def sync(headers: HttpHeaders, org: String) = {
    val request = new RequestEntity[util.Map[String, Object]](headers, HttpMethod.POST, URI.create(s"https://api.travis-ci${org}/users/sync"))
    restTemplate.exchange(request, classOf[util.Map[String, Object]])
    import scala.util.control.Breaks._
    breakable { for (i <- 0 to 30) {
      try {
        val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]])
        if (responseEntity.getStatusCode == HttpStatus.OK) {
          break
        }
      }
      catch {
        case he: HttpClientErrorException if he.getStatusCode == HttpStatus.CONFLICT => {
          print(s"  Syncing repositories" )
          Thread.sleep(1000L)
        }
        case _: Throwable => break
      }
    } }
  }

  def getRepo(headers: HttpHeaders, repo: String, org: String): Int = {
    val request = new RequestEntity[util.Map[String, Object]](headers, HttpMethod.GET, URI.create(s"https://api.travis-ci${org}/repos/${repo}"))
    val responseEntity = restTemplate.exchange(request, classOf[util.Map[String, Object]])
    responseEntity.getBody().get("id").asInstanceOf[Int]
  }

  def getAuth(token: String, org: String): HttpHeaders = {
    //val response = restTemplate.postForObject(s"https://api.travis-ci${org}/auth/github", Collections.singletonMap("github_token",
    //  token),classOf[util.Map[String, String]]);

    val headers = new HttpHeaders()
    headers.add("Authorization", s"token ${token}")
    headers.add("Content-Type", "application/json")
    headers.add("Content", "application/vnd.travis-ci.2+json")
    headers.add("User-Agent", "Travis")
    headers
  }


}
