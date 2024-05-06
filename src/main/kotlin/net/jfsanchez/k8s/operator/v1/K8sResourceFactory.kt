package net.jfsanchez.k8s.operator.v1

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ContainerBuilder
import io.kubernetes.client.openapi.models.V1DeploymentBuilder
import io.kubernetes.client.openapi.models.V1ServiceBuilder
import net.jfsanchez.k8s.operator.external.PgCluster
import net.jfsanchez.k8s.operator.v1.V1PgPoolOperator.Companion.PGPOOL_CONFIG
import net.jfsanchez.k8s.operator.v1.dto.V1PgPool

object K8sResourceFactory {
    fun newService(pgPool: V1PgPool, cluster: PgCluster) = V1ServiceBuilder()
        .withNewMetadata()
        .withName(pgPool.name())
        .withLabels<String, String>(pgPool.commonLabels())
        .addToOwnerReferences(pgPool.ownerReference())
        .endMetadata()
        .withNewSpec()
        .withType("ClusterIP")
        .addNewPort()
        .withPort(pgPool.spec.port)
        .withTargetPort(IntOrString(pgPool.spec.port))
        .endPort()
        .withSelector<String, String>(pgPool.commonLabels())
        .endSpec()
        .build()

    fun newDeployment(pgPool: V1PgPool, cluster: PgCluster) = V1DeploymentBuilder()
        .withNewMetadata()
        .withName(pgPool.name())
        .withLabels<String, String>(pgPool.commonLabels())
        .addToOwnerReferences(pgPool.ownerReference())
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewSelector()
        .addToMatchLabels(pgPool.commonLabels())
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .withLabels<String, String>(pgPool.commonLabels())
        .endMetadata()
        .withNewSpec()
        .addToContainers(buildPgPoolContainer(pgPool))
        .addNewVolume()
        .withName("${pgPool.name()}-config")
        .withNewConfigMap()
        .withName(pgPool.name())
        .endConfigMap()
        .endVolume()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build()

    fun newConfigMap(pgPool: V1PgPool, cluster: PgCluster, content: String) = V1ConfigMapBuilder()
        .withNewMetadata()
        .withName(pgPool.name())
        .withLabels<String, String>(pgPool.commonLabels())
        .addToOwnerReferences(pgPool.ownerReference())
        .endMetadata()
        .withData<String, String>(mutableMapOf(PGPOOL_CONFIG to content))
        .build()

    private fun buildPgPoolContainer(pgPool: V1PgPool): V1Container {
        return V1ContainerBuilder()
            .withImage(pgPool.spec.pgPoolImage)
            .withImagePullPolicy("IfNotPresent")
            .withName(pgPool.name())
            .addNewPort()
            .withName("pgpool-port")
            .withContainerPort(pgPool.spec.port)
            .withProtocol("TCP")
            .endPort()
            .addNewEnv()
            .withName("KUBERNETES_NAMESPACE")
            .withValue(pgPool.namespace())
            .endEnv()
            .addNewVolumeMount()
            .withName("${pgPool.name()}-config")
            .withMountPath("/config")
            .endVolumeMount()
            .build()
    }
}
