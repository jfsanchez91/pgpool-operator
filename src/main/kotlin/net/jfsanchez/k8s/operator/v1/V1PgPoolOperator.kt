package net.jfsanchez.k8s.operator.v1

import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.ModelMapper
import io.micronaut.kubernetes.client.informer.Informer
import io.micronaut.kubernetes.client.operator.Operator
import io.micronaut.kubernetes.client.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.operator.ResourceReconciler
import java.time.Duration
import net.jfsanchez.k8s.operator.ext.createNamespacedConfigMap
import net.jfsanchez.k8s.operator.ext.createNamespacedDeployment
import net.jfsanchez.k8s.operator.ext.createNamespacedService
import net.jfsanchez.k8s.operator.ext.deleteNamespacedConfigMap
import net.jfsanchez.k8s.operator.ext.deleteNamespacedDeployment
import net.jfsanchez.k8s.operator.ext.deleteNamespacedService
import net.jfsanchez.k8s.operator.ext.getNamespacedConfigMap
import net.jfsanchez.k8s.operator.ext.getNamespacedDeployment
import net.jfsanchez.k8s.operator.ext.getNamespacedService
import net.jfsanchez.k8s.operator.ext.replaceNamespacedConfigMap
import net.jfsanchez.k8s.operator.ext.replaceNamespacedDeployment
import net.jfsanchez.k8s.operator.ext.replaceNamespacedPgPool
import net.jfsanchez.k8s.operator.external.PgCluster
import net.jfsanchez.k8s.operator.external.PgClusterResourceRegistry
import net.jfsanchez.k8s.operator.v1.dto.V1PgPool
import net.jfsanchez.k8s.operator.v1.dto.V1PgPoolList
import org.slf4j.LoggerFactory

@Operator(
    name = V1PgPoolOperator.OPERATOR_NAME,
    informer = Informer(
        apiType = V1PgPool::class,
        apiListType = V1PgPoolList::class,
        resourcePlural = V1PgPool.PLURAL,
    ),
)
class V1PgPoolOperator(
    private val customObjectsApi: CustomObjectsApi,
    private val coreApi: CoreV1Api,
    private val appsApi: AppsV1Api,
) : ResourceReconciler<V1PgPool> {
    companion object {
        const val OPERATOR_NAME = "PgPoolOperator"
        const val FINALIZER_NAME = "${V1PgPool.GROUP}/$OPERATOR_NAME"
        const val PGPOOL_CONFIG = "pgpool.conf"
        const val DEFAULT_PG_PORT = 5432
        private val logger = LoggerFactory.getLogger(V1PgPoolOperator::class.java)
    }

    init {
        ModelMapper.addModelMap(
            V1PgPool.GROUP,
            V1PgPool.VERSION,
            V1PgPool.KIND,
            V1PgPool.PLURAL,
            V1PgPool::class.java,
            V1PgPoolList::class.java
        )
    }

    override fun reconcile(request: Request, lister: OperatorResourceLister<V1PgPool>): Result {
        logger.debug("Running reconcile.")
        return lister.get(request).map { pgPool ->
            logger.debug("PgPool obj definition {}.", pgPool)
            if (pgPool.isBeingDeleted()) {
                delete(pgPool)
            } else {
                createOrUpdate(pgPool)
            }
        }.orElseGet { Result(false) }
    }

    private fun createOrUpdate(pgPool: V1PgPool): Result {
        try {
            val cluster = PgClusterResourceRegistry.get(pgPool.spec.clusterName)
            if (cluster == null) {
                logger.info("There is not information available for cluster with name '{}'.", pgPool.name())
                return Result(true, Duration.ofSeconds(2))
            }

            logger.debug("Creating/updating resources.")
            val operations = createOrUpdateDeployment(pgPool, cluster) +
                             createServiceIfNeeded(pgPool, cluster)
            if (operations > 0) {
                return process(pgPool)
            } else {
                logger.debug("Everything is up-to-date. No operations needed.")
                return Result(false)
            }
        } catch (e: ApiException) {
            logger.error("Failed to create PgPool {}.", e.responseBody, e);
            return Result(false)
        }
    }

    private fun delete(pgPool: V1PgPool): Result {
        logger.debug("Deleting resources.")
        deleteConfigMap(pgPool)
        deleteDeployment(pgPool)
        deleteService(pgPool)
        pgPool.finalizeDeletion()
        try {
            customObjectsApi.replaceNamespacedPgPool(pgPool)
            return Result(false)
        } catch (e: ApiException) {
            logger.error("Failed to delete PgPool {}.", e.responseBody, e)
            return Result(true, Duration.ofSeconds(2))
        }
    }

    private fun createServiceIfNeeded(pgPool: V1PgPool, cluster: PgCluster): Int {
        return coreApi.getNamespacedService(pgPool.namespace(), pgPool.name()).map { service ->
            logger.debug("Service already exists. Skipping. (creationTimestamp={}).", service.metadata?.creationTimestamp!!)
            return@map 0
        }.orElseGet {
            logger.debug("Creating Service.")
            val service = K8sResourceFactory.newService(pgPool, cluster)
            coreApi.createNamespacedService(pgPool.namespace(), service)
            logger.info("Service '{}' created.", service.metadata?.name)
            return@orElseGet 1
        }
    }

    private fun deleteService(pgPool: V1PgPool) {
        try {
            logger.debug("Deleting Service.")
            coreApi.deleteNamespacedService(pgPool.namespace(), pgPool.name())
        } catch (e: ApiException) {
            logger.error("Failed to delete Service {}.", e.responseBody, e)
        }
    }

    private fun createOrUpdateDeployment(pgPool: V1PgPool, cluster: PgCluster): Int {
        val configUpdated = createOrUpdateConfigMap(pgPool, cluster)
        return appsApi.getNamespacedDeployment(pgPool.namespace(), pgPool.name()).map { deployment ->
            if (configUpdated) {
                logger.debug("Updating deployment.")
                appsApi.replaceNamespacedDeployment(pgPool.name(), pgPool.namespace(), deployment)
                return@map 1
            }
            logger.debug("Deployment already exists. Skipping. (creationTimestamp={}).", deployment.metadata?.creationTimestamp!!)
            return@map 0
        }.orElseGet {
            logger.debug("Creating Deployment.")
            val deployment = K8sResourceFactory.newDeployment(pgPool, cluster)
            appsApi.createNamespacedDeployment(pgPool.namespace(), deployment)
            logger.info("Deployment '{}' created.", deployment.metadata?.name)
            return@orElseGet 1
        }
    }

    private fun deleteDeployment(pgPool: V1PgPool) {
        try {
            logger.debug("Deleting Deployment.")
            appsApi.deleteNamespacedDeployment(pgPool.namespace(), pgPool.name())
        } catch (e: ApiException) {
            logger.error("Failed to delete Deployment {}.", e.responseBody, e)
        }
    }

    private fun createOrUpdateConfigMap(pgPool: V1PgPool, cluster: PgCluster): Boolean {
        return coreApi.getNamespacedConfigMap(pgPool.namespace(), pgPool.name()).map { configMap ->
            val current = configMap.data!![PGPOOL_CONFIG]
            val content = getPgPoolConfig(pgPool, cluster.numberOfInstances)
            if (!current.equals(content)) {
                logger.debug("Updating ConfigMap.")
                configMap.data = mapOf(PGPOOL_CONFIG to content)
                coreApi.replaceNamespacedConfigMap(pgPool.namespace(), pgPool.name(), configMap)
                logger.info("ConfigMap '{}' updated.", configMap.metadata?.name)
                return@map true
            }
            logger.debug("ConfigMap already exists with correct content. Skipping. (creationTimestamp={}).", configMap.metadata?.creationTimestamp!!)
            return@map false
        }.orElseGet {
            logger.debug("Creating ConfigMap.")
            val configMap = K8sResourceFactory.newConfigMap(pgPool, cluster, getPgPoolConfig(pgPool, cluster.numberOfInstances))
            coreApi.createNamespacedConfigMap(pgPool.namespace(), configMap)
            logger.info("ConfigMap '{}' created.", configMap.metadata?.name)
            return@orElseGet true
        }
    }

    private fun getPgPoolConfig(pgPool: V1PgPool, backendInstances: Int): String {
        return PgPoolConfig.from(pgPool, backendInstances).toConfigString()
    }

    private fun deleteConfigMap(pgPool: V1PgPool) {
        try {
            logger.debug("Deleting ConfigMap.")
            coreApi.deleteNamespacedConfigMap(pgPool.namespace(), pgPool.name())
        } catch (e: ApiException) {
            logger.error("Failed to delete ConfigMap {}.", e.responseBody, e)
        }
    }

    private fun process(pgPool: V1PgPool): Result {
        logger.debug("Finishing reconciliation.")
        pgPool.markAsReconciled()
        customObjectsApi.replaceNamespacedPgPool(pgPool)
        return Result(false)
    }
}
