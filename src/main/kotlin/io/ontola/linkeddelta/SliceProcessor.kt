package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate-object that match the statement from the store.
 */
class SliceProcessor : DeltaProcessor {
    override val graphIRI = createIRI("http://purl.org/linked-delta/slice")

    private val emptyStArr = emptyList<Statement>();

    override fun match(st: Statement): Boolean {
        return st.context == graphIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(emptyStArr, emptyStArr, emptyStArr)
    }
}
