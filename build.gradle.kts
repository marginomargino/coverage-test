import org.gradle.internal.logging.text.StyledTextOutput.Style
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister


buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.simpleframework:simple-xml:2.7.1")
    }
}

plugins {
    kotlin("jvm") version "1.6.10"
    `kotlin-dsl`
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
}


repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kover {
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.JACOCO)
}

val coverageReportFile = layout.buildDirectory.file("coverage/result.xml")

tasks.koverMergedXmlReport {
    isEnabled = true
    xmlReportFile.set(coverageReportFile)
    includes = listOf("io.test")
    finalizedBy(":coverageOutput")
}


tasks.register("coverageOutput", CoverageOutputTask::class.java) {
    reportFile = coverageReportFile.get().asFile
}


abstract class CoverageOutputTask : DefaultTask() {

    @InputFile
    var reportFile: File = File(".")

    @Root(name = "counter", strict = false)
    class Counter {
        @field:Attribute(name = "type")
        var type: String? = null

        @field:Attribute(name = "covered")
        var covered: String? = null

        @field:Attribute(name = "missed")
        var missed: String? = null
    }

    @Root(name = "report", strict = false)
    class Report {

        @field:ElementList(name = "counter", inline = true)
        lateinit var counters: List<Counter>
    }

    @TaskAction
    fun outputCoverage() {
        val out = services.get(StyledTextOutputFactory::class.java).create("output")
        val serializer = Persister()
        val report = serializer.read(Report::class.java, reportFile)

        out.style(Style.Info).println("--------------------------->Test Coverage<---------------------------")
        report.counters.filter { it.covered != null && it.missed != null }
            .forEach {
                val missed = it.missed!!.toDouble()
                val covered = it.covered!!.toDouble()
                val perc = String.format("%.2f", (covered / (missed + covered)) * 100)
                out.style(Style.Info).println(
                    "Type: ${it.type}. Covered: ${it.covered}. Missed: ${it.missed}. Coverage $perc%"
                )
            }
        out.style(Style.Info).println("------------------------->End Test Coverage<-------------------------")
    }

}

sourceSets {
    getByName("main") {
        java.setSrcDirs(emptyList<File>())
        kotlin.setSrcDirs(listOf("sources"))
    }
    getByName("test") {
        java.setSrcDirs(emptyList<File>())
        kotlin.setSrcDirs(listOf("tests"))
    }
}

val SourceSet.kotlin: SourceDirectorySet
    get() = project.extensions.getByType<KotlinJvmProjectExtension>().sourceSets.getByName(name).kotlin