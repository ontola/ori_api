package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate combinations from the store, disregarding the object of the statement
 */
class RemoveProcessor : BaseProcessor() {
    override val graphIRI = createIRI("http://purl.org/linked-delta/remove")

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            current.filter { s -> s.subject == st.subject && s.predicate == st.predicate },
            emptyStArr
        )
    }
}
