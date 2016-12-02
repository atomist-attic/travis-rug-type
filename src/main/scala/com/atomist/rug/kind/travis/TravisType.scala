package com.atomist.rug.kind.travis

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileArtifactMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

class TravisType (
                            evaluator: Evaluator
                          )
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override val resolvesFromNodeTypes: Set[String] = Set("project")

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
          .filter(f => f.name == (".travis.yml"))
          .map(f => new TravisTypeMutableView(f, pmv))
        )
      case fav: FileArtifactMutableView if fav.name == ".travis.yml" =>
        Some(Seq(new TravisTypeMutableView(fav.currentBackingObject, fav.parent)))
      case _ => None
    }
  }
}
