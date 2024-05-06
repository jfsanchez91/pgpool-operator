package net.jfsanchez.k8s.operator.v1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.kubernetes.client.common.KubernetesListObject
import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.models.V1ListMeta
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class V1PgPoolList(
    @JsonProperty("items")
    private val items: MutableList<V1PgPool>,
    @JsonProperty("metadata")
    private val metadata: V1ListMeta,
) : KubernetesListObject {
    override fun getApiVersion(): String {
        return V1PgPool.VERSION
    }

    override fun getKind(): String {
        return V1PgPool.KIND
    }

    override fun getMetadata(): V1ListMeta {
        return this.metadata
    }

    override fun getItems(): MutableList<out KubernetesObject> {
        return this.items
    }
}
