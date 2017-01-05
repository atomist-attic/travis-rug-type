package com.atomist.rug.kind.travis

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
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

  // Give your type a useful description
  override def description = "Travis CI type for manipulating CI configuration"

  override def viewManifest: Manifest[TravisMutableView] = manifest[TravisMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name == ".travis.yml")
          .map(f => new TravisMutableView(f, pmv, new RealTravisEndpoints))
        )
      case fav: FileMutableView if fav.name == ".travis.yml" =>
        Some(Seq(new TravisMutableView(fav.currentBackingObject, fav.parent, new RealTravisEndpoints)))
      case _ => None
    }
  }
}
