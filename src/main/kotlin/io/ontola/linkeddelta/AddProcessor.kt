package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Unconditionally adds the statement to the store, leaving any old statement
 */
class AddProcessor : BaseProcessor() {
    override val graphIRI = createIRI("http://purl.org/linked-delta/add")

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            statementWithoutContext(st),
            emptyStArr,
            emptyStArr
        )
    }
}
