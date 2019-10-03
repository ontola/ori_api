package io.ontola.linkeddelta

import io.ontola.rdfUtils.createIRI
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

abstract class BaseProcessor : DeltaProcessor {
    internal val emptyStArr = emptyList<Statement>();

    private val factory = SimpleValueFactory.getInstance()

    internal fun withoutGraph(iri: Resource): Resource? {
        if (iri is BNode) {
            return iri
        }
        val base = iri.stringValue().toHttpUrlOrNull()
        if (base === null) {
            return null
        }

        return createIRI(base.newBuilder().removeAllQueryParameters("graph").toString())
    }

    override fun match(st: Statement): Boolean {
        return withoutGraph(st.context) == graphIRI
    }

    internal fun statementWithoutContext(s: Statement): List<Statement> {
        return listOf(factory.createStatement(s.subject, s.predicate, s.`object`))
    }
}
