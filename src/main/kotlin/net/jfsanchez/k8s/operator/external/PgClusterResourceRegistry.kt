package net.jfsanchez.k8s.operator.external

import java.util.concurrent.ConcurrentHashMap
import net.jfsanchez.k8s.operator.external.dto.V1Postgresql
import org.slf4j.LoggerFactory

object PgClusterResourceRegistry {
    private val logger = LoggerFactory.getLogger(PgClusterResourceRegistry::class.java)
    private val registry = ConcurrentHashMap<String, PgCluster>()
    fun register(psql: V1Postgresql) {
        val cluster = PgCluster(
            name = psql.name(),
            namespace = psql.namespace(),
            resourceVersion = psql.resourceVersion(),
            numberOfInstances = psql.spec.numberOfInstances,
        )
        logger.info("Registering new PostgreSQL Cluster definition (cluster={}).", cluster)
        registry[cluster.name] = cluster
    }

    fun remove(psql: V1Postgresql) {
        logger.info("Removing PostgreSQL Cluster definition (id={}).", psql.id())
        registry.remove(psql.name())
    }

    fun get(name: String) = registry[name]
}
