import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import net.mamoe.mirai.console.gradle.BuildMiraiPluginTask

fun getVersionName(tagName: String) = if(tagName.startsWith("v")) tagName.substring(1) else tagName
val gitTagName: String? get() = Regex("(?<=refs/tags/).*").find(System.getenv("GITHUB_REF") ?: "")?.value
val gitCommitSha: String? get() = System.getenv("GITHUB_SHA") ?: null
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(Date()) as String

group = "com.github.asforest"
version = gitTagName?.run { getVersionName(this) } ?: System.getenv("VERSION") ?: "0.0.0"

plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.10.0"
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    implementation("org.yaml:snakeyaml:1.30")
    implementation("org.jetbrains.pty4j:pty4j:0.12.7")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

mirai {
    configureShadow {
        // 在manifest里添加信息
        manifest {
            attributes("Mirai-Plugin-Version" to archiveVersion.get())
            attributes("Git-Commit" to (gitCommitSha ?: ""))
            attributes("Compile-Time" to timestamp)
            attributes("Compile-Time-Ms" to System.currentTimeMillis())
        }

        // 打包源代码
        sourceSets.main.get().allSource.sourceDirectories.map {
            from(it) {into("project-sources/"+it.name) }
        }
    }
}

// 注册开发任务
tasks.register<Copy>("develop") {
    val buildMiraiPluginTask = tasks.withType<BuildMiraiPluginTask>()
    dependsOn(buildMiraiPluginTask)

    val archive = buildMiraiPluginTask.first().archiveFile.get().asFile

    val outputPath = System.getenv()["DBG"]?.replace("/", "\\")
    val outputDir = outputPath
        ?.run { File(this) }
        ?.run { if(!exists() || !isDirectory) null else this }

    if(outputDir != null)
    {
        from(archive).into(outputPath)
        println("Copy $archive -> $outputPath")
    }
}