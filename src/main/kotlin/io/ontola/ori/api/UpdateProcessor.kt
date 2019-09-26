/*
 * ORI API
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.ori.api

import io.ontola.activitystreams.Activity
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.ori.api.context.CtxProps
import io.ontola.ori.api.context.DocumentCtx
import io.ontola.rdfUtils.createIRI
import io.ontola.rdfUtils.tryCreateIRI
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.vocabulary.ORG
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.VCARD4
import java.nio.charset.StandardCharsets

class UpdateProcessor(
    private val record: ConsumerRecord<String, String>
) {
    suspend fun process() {
        println("[at:${record.timestamp()}] Process update: ${record.key()}, ${record.value()}")
        processHasOrganizationName(record)
        processType(record)
        println("[at:${record.timestamp()}] Finished processing update: ${record.key()}, ${record.value()}")
    }

    private suspend fun processHasOrganizationName(record: ConsumerRecord<String, String>) {
        record
            .headers()
            .lastHeader(VCARD4.HAS_ORGANIZATION_NAME.stringValue())
            ?.value()
            ?.toString(StandardCharsets.UTF_8)
            ?.let { tryCreateIRI(it) }
            ?.let {
                val docCtx = DocumentCtx(
                    CtxProps(
                        record = record,
                        iri = it
                    )
                )
                val docSet = DocumentSet(docCtx)
                val latest = docSet.findLatestDocument() ?: docSet.initNewVersion(null).save()
                val orgAS = DocumentActivityStream(docCtx.copy(version = latest.version))
                createLock(docCtx.dir()).withLock {
                    orgAS.append(
                        Activity(
                            type = AS.ADD,
                            `object` = createIRI(record.value()),
                            target = it
                        )
                    )
                    orgAS.save()
                }
            }
    }

    private suspend fun processType(record: ConsumerRecord<String, String>) {
        record
            .headers()
            .lastHeader(RDF.TYPE.stringValue())
            ?.value()
            ?.toString(StandardCharsets.UTF_8)
            ?.let { tryCreateIRI(it) }
            ?.takeIf { it == ORG.ORGANIZATION }
            ?.let {
                OrganizationsSet.withLock {
                    val organizations = OrganizationsSet()
                    organizations.add(createIRI(record.value()))
                    organizations.save()
                }
            }
    }
}
