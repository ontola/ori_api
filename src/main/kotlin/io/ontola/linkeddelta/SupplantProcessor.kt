package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Replaces an entire resource with the new representation
 */
class SupplantProcessor : BaseProcessor() {
    override val graphIRI = createIRI("http://purl.org/linked-delta/disabled")

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            statementWithoutContext(st),
            current.filter { s -> s.subject == st.subject },
            emptyStArr
        )
    }
}
