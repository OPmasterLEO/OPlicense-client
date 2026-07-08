plugins {
    id("java-library")
    id("maven-publish")
}

group = (findProperty("group") as String?)?.takeIf { it.isNotBlank() } ?: "net.opmasterleo"
version = (findProperty("version") as String?)?.takeIf { it.isNotBlank() } ?: "1.0.4"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}
