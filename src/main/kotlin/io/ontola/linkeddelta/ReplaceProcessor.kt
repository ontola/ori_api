package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate combinations which match the statement from the store and is added afterwards.
 */
class ReplaceProcessor : BaseProcessor() {
    override val graphIRI = createIRI("http://purl.org/linked-delta/replace")
    val supplantIRI = createIRI("http://purl.org/linked-delta/supplant")

    override fun match(st: Statement): Boolean {
        return st.context == graphIRI || st.context == supplantIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            emptyStArr,
            statementWithoutContext(st)
        )
    }
}
