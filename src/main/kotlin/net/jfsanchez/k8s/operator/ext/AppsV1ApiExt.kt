package net.jfsanchez.k8s.operator.ext

import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.models.V1Deployment

fun AppsV1Api.listNamespacedDeployment(namespace: String) = this.listNamespacedDeployment(
    namespace,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
)

fun AppsV1Api.getNamespacedDeployment(namespace: String, name: String) = notFoundAsNull {
    this.readNamespacedDeployment(name, namespace, null)
}

fun AppsV1Api.replaceNamespacedDeployment(namespace: String, name: String, deployment: V1Deployment) =
    this.replaceNamespacedDeployment(
        name,
        namespace,
        deployment,
        null,
        null,
        null,
        null,
    )

fun AppsV1Api.createNamespacedDeployment(namespace: String, deployment: V1Deployment) = this.createNamespacedDeployment(
    namespace,
    deployment,
    null,
    null,
    null,
    null,
)

fun AppsV1Api.deleteNamespacedDeployment(namespace: String, name: String) = this.deleteNamespacedDeployment(
    name,
    namespace,
    null,
    null,
    null,
    null,
    null,
    null,
)
