plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.23"
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.3.8"
    id("com.google.cloud.tools.jib") version "2.8.0"
    id("io.micronaut.aot") version "4.3.8"
}

version = "0.1"
group = "net.jfsanchez.k8s.operator"

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micrometer:context-propagation")
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.kubernetes:micronaut-kubernetes-client-reactor")
    implementation("io.micronaut.kubernetes:micronaut-kubernetes-discovery-client")
    implementation("io.micronaut.kubernetes:micronaut-kubernetes-operator")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.mockito:mockito-core")
}


application {
    mainClass = "net.jfsanchez.k8s.operator.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}


tasks {
    jib {
        to {
            image = "gcr.io/myapp/jib-image"
        }
    }
}
graalvmNative.toolchainDetection = false
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("net.jfsanchez.k8s.operator.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}


