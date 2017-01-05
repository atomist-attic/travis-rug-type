package com.atomist.rug.kind.travis

import java.io.StringReader
import java.security.{KeyFactory, Security}
import java.security.spec.X509EncodedKeySpec
import java.util
import java.util.Base64
import javax.crypto.Cipher

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.springframework.http.HttpHeaders
import org.yaml.snakeyaml.{DumperOptions, Yaml}

object TravisMutableView {
  val options = new DumperOptions()
  options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
  options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN)
  val yaml = new Yaml(options)

  Security.addProvider(new BouncyCastleProvider)
}

class TravisMutableView(
                         originalBackingObject: FileArtifact,
                         parent: ProjectMutableView,
                         travisEndpoints: TravisEndpoints
                       )
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  override def nodeName = "Travis"

  import TravisMutableView._

  private val mutatableContent = yaml.load(originalBackingObject.content).asInstanceOf[util.Map[String, Any]]

  override protected def currentContent: String = yaml.dump(mutatableContent).replace("'","")

  @ExportFunction(readOnly = false, description = "Enables a project for Travis CI")
  def encrypt(
               @ExportFunctionParameterDescription(name = "repo", description = "Repo slug") repo: String,
               @ExportFunctionParameterDescription(name = "github_token", description = "GitHub Token") githubToken: String,
               @ExportFunctionParameterDescription(name = "org", description = ".com or .org") org: String,
               @ExportFunctionParameterDescription(name = "content", description = "Content") content: String
             ): Unit = {

    val secured = encryptString(repo, githubToken, org, content)

    if (mutatableContent.containsKey("env") && mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].containsKey("global")) {
      if (mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].get("global").asInstanceOf[util.List[String]] != null) {
        val global = mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].get("global").asInstanceOf[util.List[String]]
        global.add(secured)
      }
      else {
        val global: util.List[String] = new util.ArrayList[String]()
        mutatableContent.get("env").asInstanceOf[util.Map[String, Any]].put("global", global)
        global.add(secured)
      }
    }
    else {
      val env: util.Map[String, Any] = new util.HashMap[String, Any]()
      mutatableContent.put("env", env)
      val global: util.List[String] = new util.ArrayList[String]()
      env.put("global", global)
      global.add(secured)
    }

    content match {
      case _: String if content.contains("=") => print(s"  Added encrypted value for ${content.substring(0, content.indexOf("="))}")
      case _ => print("  Added encrypted value")
    }

  }

  @ExportFunction(readOnly = true, description = "Enables a project for Travis CI")
  def enable(
              @ExportFunctionParameterDescription(name = "repo", description = "Repo slug") repo: String,
              @ExportFunctionParameterDescription(name = "github_token", description = "GitHub Token") token: String,
              @ExportFunctionParameterDescription(name = "org", description = ".com or .org") org: String
            ): Unit = {
    val enableTravis = true
    hook(enableTravis, repo, token, org)
    print("  Enabled repository" )
  }

  @ExportFunction(readOnly = true, description = "Disables a project for Travis CI")
  def disable(
               @ExportFunctionParameterDescription(name = "repo", description = "Repo slug") repo: String,
               @ExportFunctionParameterDescription(name = "github_token", description = "GitHub Token") token: String,
               @ExportFunctionParameterDescription(name = "org", description = ".com or .org") org: String
             ): Unit = {
    val disableTravis = false
    hook(disableTravis, repo, token, org)
    print("  Disabled repository" )
  }

  private[travis] def hook(active: Boolean, repo: String, githubToken: String, org: String): Unit = {
    val api: TravisAPIEndpoint = TravisAPIEndpoint.stringToTravisEndpoint(org)
    val token: String = travisEndpoints.postAuthGitHub(api, githubToken)
    val headers: HttpHeaders = TravisEndpoints.authHeaders(api, token)

    travisEndpoints.postUsersSync(api, headers)

    val id: Int = travisEndpoints.getRepo(api, headers, repo)

    val hook = new util.HashMap[String, Any]()
    hook.put("id", id)
    hook.put("active", active)
    val body = new util.HashMap[String, Object]()
    body.put("hook", hook)

    travisEndpoints.putHook(api, headers, body)
  }

  private[travis] def encryptString(repo: String, githubToken: String, org: String, content: String): String = {
    val api: TravisAPIEndpoint = TravisAPIEndpoint.stringToTravisEndpoint(org)
    val token: String = travisEndpoints.postAuthGitHub(api, githubToken)
    val headers: HttpHeaders = TravisEndpoints.authHeaders(api, token)

    val key = travisEndpoints.getRepoKey(api, headers, repo)

    val parser = new PEMParser(new StringReader(key))
    val ob = parser.readObject().asInstanceOf[SubjectPublicKeyInfo]

    val pubKeySpec = new X509EncodedKeySpec(ob.getEncoded())
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKey = keyFactory.generatePublic(pubKeySpec)

    val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)

    s"secure: ${Base64.getEncoder.encodeToString(rsaCipher.doFinal(content.getBytes()))}"
  }

}
