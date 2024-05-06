package net.jfsanchez.k8s.operator.ext

import io.kubernetes.client.openapi.ApiException
import java.util.Optional
import java.util.function.Supplier

inline fun <reified T> notFoundAsNull(supplier: Supplier<T>): Optional<out T> = try {
    Optional.ofNullable(supplier.get())
} catch (e: ApiException) {
    if (e.code == 404) Optional.empty<T>() else throw e
}
