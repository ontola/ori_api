package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all the statements of a subject from the store, disregarding the predicate and object of the statement
 */
class PurgeProcessor : DeltaProcessor {
    override val graphIRI = createIRI("http://purl.org/linked-delta/purge")

    private val emptyStArr = emptyList<Statement>();

    override fun match(st: Statement): Boolean {
        return st.context == graphIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            current.filter { s -> s.subject == st.subject },
            emptyStArr
        )
    }
}
