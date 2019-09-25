package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate-object that match the statement from the store.
 */
class SliceProcessor : BaseProcessor() {
    override val graphIRI = createIRI("http://purl.org/linked-delta/slice")

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            current.filter { s -> s.subject == st.subject && s.predicate == st.predicate && s.`object` == st.`object` },
            emptyStArr
        )
    }
}
