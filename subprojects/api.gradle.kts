import kotlinx.dom.elements
import kotlinx.dom.parseXml
import kotlinx.dom.search

plugins {
    java
}

repositories {
    loadRepositories(File(project.projectDir, "pom.xml"))
}

dependencies {
    loadDependencies(File(project.projectDir, "pom.xml"))
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes("Automatic-Module-Name" to "org.bukkit")
    }
}

fun RepositoryHandler.loadRepositories(pomFile: File) {
    val dom = parseXml(pomFile)
    val repositoriesBlock = dom.search("repositories").firstOrNull() ?: return

    // Load repositories
    repositoriesBlock.elements("repository").forEach { repositoryElem ->
        val url = repositoryElem.search("url").firstOrNull()?.textContent ?: return@forEach
        maven(url)
    }
}

fun DependencyHandlerScope.loadDependencies(pomFile: File) {
    val dom = parseXml(pomFile)

    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return

    // Load dependencies
    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent
        val version = dependencyElem.search("version").firstOrNull()!!.textContent
        val scope = dependencyElem.search("scope").firstOrNull()?.textContent
        val classifier = dependencyElem.search("classifier").firstOrNull()?.textContent

        val dependencyString = "${groupId}:${artifactId}:${version}${classifier?.run {":$this" } ?: ""}"
        logger.debug("Read dependency '{}' from '{}'", dependencyString, pomFile.absolutePath)

        when (scope) {
            "compile", null -> compile(dependencyString)
            "provided" -> {
                compileOnly(dependencyString)
                testImplementation(dependencyString) // TODO: Bukkit quirk? or Maven scope mapping? No clue
            }
            "runtime" -> runtimeOnly(dependencyString)
            "test" -> testImplementation(dependencyString)
        }
    }
}