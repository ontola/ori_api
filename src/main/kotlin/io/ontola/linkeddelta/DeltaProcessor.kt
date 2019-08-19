package io.ontola.linkeddelta

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

interface DeltaProcessor {
    val graphIRI: IRI

    fun match(st: Statement): Boolean

    fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult
}
