package io.ontola.linkeddelta

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.LinkedHashModel

val processors = listOf(
    SupplantProcessor(),
    AddProcessor(),
    ReplaceProcessor(),
    RemoveProcessor(),
    PurgeProcessor(),
    SliceProcessor()
)

private fun replaceMatches(store: Model, replacements: List<Statement>) {
    for (st in replacements) {
        store.removeAll(store.filter(st.subject, st.predicate, null, null).toList())
    }

    store.addAll(replacements)
}

/**
 * Takes a model and delta and returns the result of applying the delta on the model.
 */
fun applyDelta(processors: List<DeltaProcessor>, current: Model, delta: Model): Model {
    val result = LinkedHashModel(current)

    val addable = mutableListOf<Statement>();
    val replaceable = mutableListOf<Statement>();
    val removable = mutableListOf<Statement>();

    for (statement in delta) {
        val processor = processors.find { p -> p.match(statement) }
        if (processor != null) {
            val res = processor.process(current, delta, statement)
            addable.addAll(res.addable)
            replaceable.addAll(res.replaceable)
            removable.addAll(res.removable)
        } else {
            throw Exception("No processor for graph ${statement.context} (statement was $statement)")
        }
    }

    result.removeAll(removable);
    replaceMatches(result, replaceable)
    result.addAll(addable)

    return result
}
