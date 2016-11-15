package com.atomist.rug

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{ModificationAttempt, ProjectEditor, SuccessfulModification}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, StringFileArtifact}
import org.scalatest.Matchers

object TestUtils extends Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  def doModification(program: String,
                     as: ArtifactSource,
                     backingAs: ArtifactSource,
                     poa: ProjectOperationArguments): ArtifactSource = {

    attemptModification(program, as, backingAs, poa) match {
      case sm: SuccessfulModification =>
        // show(sm.result)
        sm.result
    }
  }

  def attemptModification(program: String,
                          as: ArtifactSource,
                          backingAs: ArtifactSource,
                          poa: ProjectOperationArguments): ModificationAttempt = {

    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)

    val eds = runtime.create(backingAs +
      StringFileArtifact(atomistConfig.defaultRugFilepath, program), None)
    eds.size should be >= (1)
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, poa)
  }
}
