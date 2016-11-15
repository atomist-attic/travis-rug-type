package com.atomist.rug.kind.travis

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.pom.PomMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.Evaluator
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConversions._

class TravisType @Autowired()(
                            evaluator: Evaluator
                          )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  // Give your type a name to identify it in Rug when using with
  override def name = "travis"

  // Give your type a useful description
  override def description = "Travis Type file"

  override def viewManifest: Manifest[TravisTypeMutableView] = manifest[TravisTypeMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.contains(".travis"))
          .map(f => new TravisTypeMutableView(f, pmv))
        )
      case _ => None
    }
  }
}