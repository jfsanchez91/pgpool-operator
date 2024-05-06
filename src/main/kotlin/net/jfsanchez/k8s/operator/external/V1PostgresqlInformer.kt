package net.jfsanchez.k8s.operator

import io.kubernetes.client.informer.ResourceEventHandler
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.micronaut.kubernetes.client.informer.Informer
import net.jfsanchez.k8s.operator.external.PgClusterResourceRegistry
import net.jfsanchez.k8s.operator.external.dto.V1Postgresql
import net.jfsanchez.k8s.operator.external.dto.V1PostgresqlList
import org.slf4j.LoggerFactory

@Informer(apiType = V1Postgresql::class, apiListType = V1PostgresqlList::class)
class V1PostgresqlInformer(
    private val appsV1Api: AppsV1Api,
    private val coreV1Api: CoreV1Api,
) : ResourceEventHandler<V1Postgresql> {
    companion object {
        val logger = LoggerFactory.getLogger(V1PostgresqlInformer::class.java)
    }

    init {
        logger.info("Initializing {}.", this::class.qualifiedName)
    }

    override fun onAdd(psql: V1Postgresql) {
        logger.debug("new V1Postgresql object added (obj={}).", psql)
        PgClusterResourceRegistry.register(psql)
    }

    override fun onUpdate(oldPsql: V1Postgresql, newPsql: V1Postgresql) {
        logger.debug("V1Postgresql object updated (oldObj={}, newObj={}).", oldPsql, newPsql)
    }

    override fun onDelete(psql: V1Postgresql, flag: Boolean) {
        logger.debug("V1Postgresql object deleted (obj={}, deletedFinalStateUnknown={}).", psql, flag)
    }
}
