package net.jfsanchez.k8s.operator.external.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class V1Postgresql(
    @JsonProperty("apiVersion")
    private val apiVersion: String,
    @JsonProperty("kind")
    private val kind: String,
    @JsonProperty("metadata")
    private val metadata: V1ObjectMeta,
    @JsonProperty("spec")
    val spec: V1PostgresqlSpec,
) : KubernetesObject {
    init {
        if (metadata.annotations == null) {
            getMetadata().annotations = HashMap()
        }
    }

    companion object {
        const val API_VERSION = "v1"
        const val KIND = "Postgresql"
        const val GROUP = "acid.zalan.do"
        const val PLURAL = "postgresqls"
    }

    override fun getApiVersion(): String {
        return this.apiVersion
    }

    override fun getKind(): String {
        return this.kind
    }

    override fun getMetadata(): V1ObjectMeta {
        return this.metadata
    }

    @Serdeable
    data class V1PostgresqlSpec(
        @JsonProperty("numberOfInstances")
        val numberOfInstances: Int,
    )

    fun name() = metadata.name ?: ""
    fun namespace() = metadata.namespace ?: ""
    fun resourceVersion() = metadata.resourceVersion ?: ""
    fun id(): String = "${this.name()}::${this.name()}::${this.resourceVersion()}"
}
