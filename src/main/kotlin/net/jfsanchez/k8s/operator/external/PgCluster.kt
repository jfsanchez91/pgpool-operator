package net.jfsanchez.k8s.operator.external

data class PgCluster(
    val name: String,
    val namespace: String,
    val resourceVersion: String,
    val numberOfInstances: Int,
)
