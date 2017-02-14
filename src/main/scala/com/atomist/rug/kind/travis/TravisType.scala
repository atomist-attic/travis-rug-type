package com.atomist.rug.kind.travis

import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class TravisType (evaluator: Evaluator)
  extends Type(evaluator)
    with ChildResolver
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "Travis CI type for manipulating CI configuration"

  override def runtimeClass = classOf[TravisMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[TreeNode]] = context match {
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
