package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Unconditionally adds the statement to the store, leaving any old statement
 */
class AddProcessor : DeltaProcessor {
    override val graphIRI = createIRI("http://purl.org/linked-delta/add")

    private val emptyStArr = emptyList<Statement>();

    override fun match(st: Statement): Boolean {
        return st.context == graphIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(emptyStArr, emptyStArr, emptyStArr)
    }
}
