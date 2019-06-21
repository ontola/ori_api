package io.ontola.ori.api

import com.github.jsonldjava.core.*
import com.github.jsonldjava.utils.JsonUtils
import com.google.common.io.Resources
import io.ontola.activitystreams.vocabulary.AS
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.RDFWriter
import org.eclipse.rdf4j.rio.RioSetting
import org.eclipse.rdf4j.rio.WriterConfig
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import org.eclipse.rdf4j.rio.jsonld.JSONLDHierarchicalProcessor
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.*

private class ModelParser : RDFParser {
    override fun parse(input: Any?): RDFDataset {
        if (input !is Model) {
            throw IllegalArgumentException("Model parser must be given a rdf4j.model.Model")
        }

        val dataset = RDFDataset()
        for (st in input) {
            val subj = parseValue(st.subject)
            val pred = parseValue(st.predicate)
            val obj = st.`object`

            if (obj is Literal) {
                dataset.addTriple(
                    subj,
                    pred,
                    obj.stringValue(),
                    obj.datatype.stringValue(),
                    obj.language.orElse(null)
                )
            } else {
                dataset.addTriple(subj, pred, parseValue(obj))
            }
        }

        return dataset
    }

    private fun parseValue(value: Value): String {
        return if (value is Resource) {
            value.toString()
        } else {
            value.stringValue()
        }
    }
}

/**
 * Patches some issues with regards to formatting and top-level @context string values for optimal AS compatibility
 */
class JSONLDWriter : RDFWriter {
    private var config: WriterConfig
    private val writer: Writer
    private val context = Context(JsonLdOptions(null))

    private val model = LinkedHashModel()
    private val collector = StatementCollector(model)

    constructor(out: OutputStream, config: WriterConfig) {
        this.config = config
        this.writer = OutputStreamWriter(out)
    }

    constructor(writer: Writer, config: WriterConfig) {
        this.config = config
        this.writer = writer
    }

    override fun getWriterConfig(): WriterConfig {
        return config
    }

    override fun setWriterConfig(config: WriterConfig?): RDFWriter {
        if (config != null) {
            this.config = config
        }

        return this
    }

    override fun getSupportedSettings(): MutableCollection<RioSetting<*>> {
        return Collections.emptyList()
    }

    override fun <T : Any?> set(setting: RioSetting<T>?, value: T): RDFWriter {
        return JSONLDWriter(writer, config.set(setting, value))
    }

    override fun getRDFFormat(): RDFFormat {
        return RDFFormat.JSONLD
    }

    override fun handleNamespace(prefix: String?, uri: String?) {
        context[prefix] = uri
    }

    override fun handleComment(comment: String?) {
        /* JSON has no comments */
    }

    override fun handleStatement(st: Statement?) {
        collector.handleStatement(st)
    }

    override fun startRDF() {}

    override fun endRDF() {
        val options = JsonLdOptions()
        options.useRdfType = false
        options.useNativeTypes = true
        val loader = DocumentLoader()
        val asCtx = Resources.getResource("activitystreams.jsonld").readText()
        loader.addInjectedDoc(
            AS.NAMESPACE,
            asCtx
        )
        options.documentLoader = loader
        context.parse(HashMap<String, Any>(), listOf(asCtx))

        var obj: Any = JsonLdProcessor.fromRDF(model, options, ModelParser())
        if (writerConfig.get(JSONLDSettings.HIERARCHICAL_VIEW)) {
            obj = JSONLDHierarchicalProcessor.fromJsonLdObject(obj)
        }
        obj = JsonLdProcessor.compact(obj, context, options)
        if (context[JsonLdConsts.CONTEXT] != null) {
            obj[JsonLdConsts.CONTEXT] = context[JsonLdConsts.CONTEXT]
        }
        JsonUtils.writePrettyPrint(writer, obj)
    }
}
