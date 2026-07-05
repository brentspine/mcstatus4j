plugins {
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.spotless)
}

group = "de.brentspine"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dnsjava)
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
    }
    kotlinGradle {
        target("*.gradle.kts")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("mcstatus4j")
                description.set("A Java library for querying Minecraft servers (Java and Bedrock editions)")
                url.set("https://github.com/brentspine/mcstatus4j")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("brentspine")
                        name.set("Brentspine")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/brentspine/mcstatus4j.git")
                    developerConnection.set("scm:git:ssh://github.com/brentspine/mcstatus4j.git")
                    url.set("https://github.com/brentspine/mcstatus4j")
                }
            }
        }
    }
}
