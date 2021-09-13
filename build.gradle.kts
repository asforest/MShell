plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7.0"
}

group = "com.github.asforest"
version = "0.0.1"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
//    implementation("org.yaml:snakeyaml:1.29")
    implementation("com.esotericsoftware.yamlbeans:yamlbeans:1.15")
}

tasks.register("buildTest", Copy::class) {
    dependsOn(tasks.named("clean"))
    dependsOn(tasks.named("buildPlugin"))
}

// only for developing
tasks.register("buildWithCopy", Copy::class) {
    dependsOn(tasks.named("buildPlugin"))
    from("build/mirai/MShell-0.0.1.mirai.jar")
    into("D:/mirai/plugins")
}