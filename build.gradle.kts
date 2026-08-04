plugins {
    id("java-library")
    id("maven-publish")
}

group = (findProperty("group") as String?)?.takeIf { it.isNotBlank() } ?: "net.opmasterleo"

val baseVersion = (findProperty("version") as String?)
    ?.takeIf { it.isNotBlank() && it != Project.DEFAULT_VERSION }
    ?: "2.0.0"

val reposiliteTarget = (findProperty("reposilite.target") as String?)
    ?.trim()
    ?.lowercase()
    ?: "releases"

val publishSnapshots = reposiliteTarget == "snapshots"

version = when {
    publishSnapshots && !baseVersion.endsWith("-SNAPSHOT") -> "$baseVersion-SNAPSHOT"
    !publishSnapshots && baseVersion.endsWith("-SNAPSHOT") -> baseVersion.removeSuffix("-SNAPSHOT")
    else -> baseVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    exclude("**/internal/**")
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
            from(components["java"])
            pom {
                name.set("OPLicense Client")
                description.set("Java license SDK for validating against a self-hosted OPLicense backend.")
                url.set("https://github.com/OPmasterLEO/OPlicense-client")
                developers {
                    developer {
                        id.set("opmasterleo")
                        name.set("OPmasterLEO")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "Reposilite"
            url = uri(
                if (publishSnapshots) {
                    "https://repo.mastersmp.net/snapshots"
                } else {
                    "https://repo.mastersmp.net/releases"
                }
            )
            credentials(PasswordCredentials::class) {
                username = project.findProperty("reposilite.user") as String?
                    ?: System.getenv("REPOSILITE_USER")
                    ?: ""
                password = project.findProperty("reposilite.token") as String?
                    ?: System.getenv("REPOSILITE_TOKEN")
                    ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst {
        val user = project.findProperty("reposilite.user") as String?
            ?: System.getenv("REPOSILITE_USER")
        val token = project.findProperty("reposilite.token") as String?
            ?: System.getenv("REPOSILITE_TOKEN")
        require(!user.isNullOrBlank()) { "Missing reposilite.user / REPOSILITE_USER (token name)" }
        require(!token.isNullOrBlank()) { "Missing reposilite.token / REPOSILITE_TOKEN (token secret)" }
        logger.lifecycle(
            "Publishing ${project.group}:${rootProject.name}:${project.version} " +
                "as user='$user' (secret length=${token.length}) " +
                "to ${if (publishSnapshots) "snapshots" else "releases"}"
        )
    }
}

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}

tasks.register("printReposiliteTarget") {
    doLast {
        println(if (publishSnapshots) "snapshots" else "releases")
    }
}
