package net.jfsanchez.k8s.operator.ext

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1Service

fun CoreV1Api.listNamespacedService(namespace: String) = this.listNamespacedService(
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

fun CoreV1Api.getNamespacedService(namespace: String, name: String) = notFoundAsNull {
    this.readNamespacedService(name, namespace, null)
}

fun CoreV1Api.existNamespacedService(namespace: String, name: String) = this.listNamespacedService(namespace)
    .items.any { service -> service.metadata?.name.equals(name) }

fun CoreV1Api.getNamespacedConfigMap(namespace: String, name: String) = notFoundAsNull {
    this.readNamespacedConfigMap(name, namespace, null)
}

fun CoreV1Api.createNamespacedService(namespace: String, service: V1Service) = this.createNamespacedService(
    namespace,
    service,
    null,
    null,
    null,
    null,
)

fun CoreV1Api.deleteNamespacedService(namespace: String, name: String) = this.deleteNamespacedService(
    name,
    namespace,
    null,
    null,
    null,
    null,
    null,
    null
)

fun CoreV1Api.deleteNamespacedConfigMap(namespace: String, name: String) = this.deleteNamespacedConfigMap(
    name,
    namespace,
    null,
    null,
    null,
    null,
    null,
    null,
)

fun CoreV1Api.listNamespacedConfigMap(namespace: String) = this.listNamespacedConfigMap(
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

fun CoreV1Api.existNamespacedConfigMap(namespace: String, name: String) = this.listNamespacedConfigMap(namespace)
    .items.any { configMap -> configMap.metadata!!.name.equals(name) }

fun CoreV1Api.createNamespacedConfigMap(namespace: String, configMap: V1ConfigMap) = this.createNamespacedConfigMap(
    namespace,
    configMap,
    null,
    null,
    null,
    null,
)

fun CoreV1Api.replaceNamespacedConfigMap(namespace: String, name: String, configMap: V1ConfigMap) =
    this.replaceNamespacedConfigMap(
        name,
        namespace,
        configMap,
        null,
        null,
        null,
        null,
    )

