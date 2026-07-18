plugins {
    id("java-library")
    id("maven-publish")
}

group = (findProperty("group") as String?)?.takeIf { it.isNotBlank() } ?: "net.opmasterleo"
version = (findProperty("version") as String?)?.takeIf { it.isNotBlank() } ?: "1.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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
        }
    }
}
