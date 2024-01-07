import java.text.SimpleDateFormat
import java.util.Date
import net.mamoe.mirai.console.gradle.BuildMiraiPluginV2

fun getVersionName(tagName: String) = if(tagName.startsWith("v")) tagName.substring(1) else tagName
val gitTagName: String? get() = Regex("(?<=refs/tags/).*").find(System.getenv("GITHUB_REF") ?: "")?.value
val gitCommitSha: String? get() = System.getenv("GITHUB_SHA") ?: null
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(Date()) as String

group = "com.github.asforest"
version = gitTagName?.run { getVersionName(this) } ?: System.getenv("VERSION") ?: "0.0.0"

plugins {
    val kotlinVersion = "1.6.10"
    val miraiVersion = "2.12.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version miraiVersion
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    implementation("org.yaml:snakeyaml:1.33")
    implementation("org.jetbrains.pty4j:pty4j:0.12.10")

    // 告知 mirai-console 在打包插件时包含此依赖，不要剥离掉此依赖；无需包含版本号
    "shadowLink"("org.jetbrains.pty4j:pty4j")
    implementation(kotlin("stdlib-jdk8"))
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

afterEvaluate {
    tasks.named<BuildMiraiPluginV2>("buildPlugin") {
        manifest {
            attributes("Mirai-Plugin-Version" to archiveVersion.get())
            attributes("Git-Commit" to (gitCommitSha ?: ""))
            attributes("Compile-Time" to timestamp)
        }
    }
}

// 注册开发任务
tasks.register<Copy>("develop") {
    val buildMiraiPluginTask = tasks.named<BuildMiraiPluginV2>("buildPlugin")
    dependsOn(buildMiraiPluginTask)

    val archive = buildMiraiPluginTask.get().archiveFile.get().asFile
    val outputPath = System.getenv()["DBG"]?.replace("/", "\\")
    val outputDir = outputPath?.run { File(this) }
        ?.run { if(!exists() || !isDirectory) null else this }

    if(outputDir != null)
    {
        from(archive).into(outputPath)
        println("Copy $archive -> $outputPath")
    }
}