package net.jfsanchez.k8s.operator.ext

import io.kubernetes.client.openapi.apis.CustomObjectsApi
import net.jfsanchez.k8s.operator.v1.dto.V1PgPool

fun CustomObjectsApi.replaceNamespacedPgPool(pgPool: V1PgPool) = this.replaceNamespacedCustomObject(
    V1PgPool.GROUP,
    V1PgPool.VERSION,
    pgPool.namespace(),
    V1PgPool.PLURAL,
    pgPool.name(),
    pgPool,
    null,
    null,
)
