package com.atomist.rug.kind.travis

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.ExportFunction
import com.atomist.source.FileArtifact

object TravisTypeMutableView {

  // Put your constants here
}

trait TravisTypeMutableViewNonMutatingFunctions {

  import TravisTypeMutableView._

  // Add your non mutating exported Rug Type functions here.

  @ExportFunction(readOnly = true, description = "Return a custom property")
  def customProperty: String = "myvalue"
}

trait TravisTypeMutableViewMutatingFunctions {

  import TravisTypeMutableView._

  // Add your mutating exported Rug Type functions here.
}

class TravisTypeMutableView(
                      originalBackingObject: FileArtifact,
                      parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent)
    with TravisTypeMutableViewNonMutatingFunctions
    with TravisTypeMutableViewMutatingFunctions {

  private var mutatableContent = originalBackingObject.content

  override protected def currentContent: String = mutatableContent
}
