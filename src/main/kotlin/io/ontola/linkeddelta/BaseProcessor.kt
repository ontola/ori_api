package io.ontola.linkeddelta

import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

abstract class BaseProcessor : DeltaProcessor {
    internal val emptyStArr = emptyList<Statement>();

    private val factory = SimpleValueFactory.getInstance()

    override fun match(st: Statement): Boolean {
        return st.context == graphIRI
    }

    internal fun statementWithoutContext(s: Statement): List<Statement> {
        return listOf(factory.createStatement(s.subject, s.predicate, s.`object`))
    }
}
