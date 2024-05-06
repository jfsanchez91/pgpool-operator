package net.jfsanchez.k8s.operator.v1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1OwnerReference
import io.kubernetes.client.openapi.models.V1OwnerReferenceBuilder
import io.micronaut.serde.annotation.Serdeable
import java.time.OffsetDateTime
import net.jfsanchez.k8s.operator.v1.V1PgPoolOperator

@Serdeable
data class V1PgPool(
    @JsonProperty("apiVersion")
    private val apiVersion: String,
    @JsonProperty("kind")
    private val kind: String,
    @JsonProperty("metadata")
    private val metadata: V1ObjectMeta,
    @JsonProperty("spec")
    val spec: V1PgPoolSpec,
) : KubernetesObject {
    companion object {
        const val VERSION = "v1"
        const val KIND = "PgPool"
        const val GROUP = "pgpool-operator.jfsanchez.net"
        const val SINGULAR = "pgpool"
        const val PLURAL = "pgpools"
        const val RECONCILED_ANNOTATION = "$GROUP/reconciled"
        const val RECONCILIATION_TIMESTAMP_ANNOTATION = "$GROUP/reconciled-at"
    }

    init {
        if (metadata.annotations == null) {
            metadata.annotations = mutableMapOf()
        }
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
    data class V1PgPoolSpec(
        @JsonProperty("pgPoolImage")
        val pgPoolImage: String = "pgpool/pgpool:4.4.3",
        @JsonProperty("clusterName")
        val clusterName: String,
        @JsonProperty("port")
        val port: Int,
        @JsonProperty("maxPoolSize")
        val maxPoolSize: Int,
        @JsonProperty("numInitChildren")
        val numInitChildren: Int,
        @JsonProperty("healthCheckPeriod")
        val healthCheckPeriod: Int,
        @JsonProperty("childLifeTime")
        val childLifeTime: Int,
        @JsonProperty("childMaxConnections")
        val childMaxConnections: Int,
        @JsonProperty("connectionLifeTime")
        val connectionLifeTime: Int,
        @JsonProperty("clientIdleLimit")
        val clientIdleLimit: Int,
        @JsonProperty("srCheckPeriod")
        val srCheckPeriod: Int,
        @JsonProperty("connectionCache")
        val connectionCache: Boolean,
        @JsonProperty("loadBalanceMode")
        val loadBalanceMode: Boolean,
        @JsonProperty("ssl")
        val ssl: Boolean,
        @JsonProperty("failoverOnBackendError")
        val failoverOnBackendError: Boolean,
    )

    fun name(): String = metadata.name ?: ""
    fun namespace(): String = metadata.namespace ?: ""
    fun commonLabels(): Map<String, String> = mapOf(
        "app.kubernetes.io/name" to this.name(),
        "app" to this.name(),
        "app.kubernetes.io/version" to "1.0",
    )

    fun isBeingDeleted(): Boolean = metadata.deletionTimestamp != null
    fun finalizeDeletion() = metadata.finalizers?.clear()
    fun markAsReconciled() {
        metadata.annotations?.put(RECONCILED_ANNOTATION, true.toString())
        metadata.annotations?.put(RECONCILIATION_TIMESTAMP_ANNOTATION, OffsetDateTime.now().toString())
        metadata.addFinalizersItem(V1PgPoolOperator.FINALIZER_NAME)
    }

    fun isReconciled(): Boolean = metadata.annotations?.containsKey(RECONCILED_ANNOTATION) ?: false
    fun ownerReference(): V1OwnerReference {
        return V1OwnerReferenceBuilder()
            .withName(this.name())
            .withApiVersion(VERSION)
            .withKind(KIND)
            .withController(true)
            .withUid(metadata.uid)
            .withBlockOwnerDeletion(true)
            .build()
    }
}
