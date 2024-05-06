package net.jfsanchez.k8s.operator.v1

import net.jfsanchez.k8s.operator.v1.V1PgPoolOperator.Companion.DEFAULT_PG_PORT
import net.jfsanchez.k8s.operator.v1.dto.V1PgPool

data class PgPoolConfig(
    val listenAddress: String = "*",
    val port: Int = 9999,
    val enableSsl: Boolean = true,
    val enableLoadBalanceMode: Boolean = true,
    val initChildrenSize: Int = 32,
    val maxPoolSize: Int = 4,
    val childMaxConnections: Int = 0,
    val enableConnectionCache: Boolean = true,
    val healthCheckPeriod: Int = 0,
    val childLifeTime: Int = 300,
    val connectionLifeTime: Int = 0,
    val clientIdleLimit: Int = 0,
    val srCheckPeriod: Int = 0,
    val enableFailOverOnBackendError: Boolean = true,
    val backends: Set<PgPoolBackendConfig>,
) {
    companion object {
        fun from(pgPool: V1PgPool, backendInstances: Int): PgPoolConfig {
            val backends = mutableSetOf<PgPoolBackendConfig>()
            if (backendInstances == 1) backends.add(
                PgPoolBackendConfig(
                    hostname = pgPool.spec.clusterName,
                    port = DEFAULT_PG_PORT,
                    isPrimary = true,
                )
            ) else if (backendInstances > 1) backends.add(
                PgPoolBackendConfig(
                    hostname = "${pgPool.spec.clusterName}-repl",
                    port = DEFAULT_PG_PORT,
                    isPrimary = false,
                )
            )
            return PgPoolConfig(
                listenAddress = "*",
                port = pgPool.spec.port,
                enableSsl = pgPool.spec.ssl,
                enableLoadBalanceMode = pgPool.spec.loadBalanceMode,
                initChildrenSize = pgPool.spec.numInitChildren,
                maxPoolSize = pgPool.spec.maxPoolSize,
                childMaxConnections = pgPool.spec.childMaxConnections,
                enableConnectionCache = pgPool.spec.connectionCache,
                healthCheckPeriod = pgPool.spec.healthCheckPeriod,
                childLifeTime = pgPool.spec.childLifeTime,
                connectionLifeTime = pgPool.spec.connectionLifeTime,
                clientIdleLimit = pgPool.spec.clientIdleLimit,
                srCheckPeriod = pgPool.spec.srCheckPeriod,
                enableFailOverOnBackendError = pgPool.spec.failoverOnBackendError,
                backends = backends,
            )
        }
    }

    data class PgPoolBackendConfig(
        val hostname: String,
        val port: Int,
        val isPrimary: Boolean,
        val weight: Int = 1,
    ) {
        fun toConfigString(index: Int) = StringBuilder()
            .appendLine("backend_hostname$index = '$hostname'")
            .appendLine("backend_port$index = $port")
            .appendLine("backend_flag$index = '${if (isPrimary) "ALWAYS_PRIMARY|" else ""}DISALLOW_TO_FAILOVER'")
            .appendLine("backend_weight$index = $weight")
            .toString()
    }

    fun toConfigString(): String {
        val config = StringBuilder()
            .appendLine("listen_addresses = '$listenAddress'")
            .appendLine("port = $port")
            .appendLine("socket_dir = '/var/run/pgpool'")
            .appendLine("enable_pool_hba = off")
            .appendLine("pool_passwd = ''")
            .appendLine("allow_clear_text_frontend_auth = on")
            .appendLine("auth_method = trust")
//            .appendLine("pcp_listen_addresses = '*'")
//            .appendLine("pcp_port = 9898")
//            .appendLine("pcp_socket_dir = '/var/run/pgpool'")
//            .appendLine("enable_pool_hba = on")
            .appendLine("log_min_messages = warning")
            .appendLine("sr_check_period = $srCheckPeriod")
            .appendLine("health_check_period = $healthCheckPeriod")
            .appendLine("backend_clustering_mode = 'streaming_replication'")
            .appendLine("num_init_children = $initChildrenSize")
            .appendLine("max_pool = $maxPoolSize")
            .appendLine("child_life_time = $childLifeTime")
            .appendLine("child_max_connections = $childMaxConnections")
            .appendLine("connection_life_time = $connectionLifeTime")
            .appendLine("client_idle_limit = $clientIdleLimit")
            .appendLine("connection_cache = ${enableConnectionCache.onOrOff()}")
            .appendLine("load_balance_mode = ${enableLoadBalanceMode.onOrOff()}")
            .appendLine("ssl = ${enableSsl.onOrOff()}")
            .appendLine("failover_on_backend_error = ${enableFailOverOnBackendError.onOrOff()}")
        backends.forEachIndexed { index, backend -> config.appendLine(backend.toConfigString(index)) }
        return config.toString()
    }

    private fun Boolean.onOrOff() = if (this) "on" else "off"
}
