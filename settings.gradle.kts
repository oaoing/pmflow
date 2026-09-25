plugins {
	// JDK 25 미설치 시 Gradle이 자동으로 받아오도록 (toolchain auto-provisioning)
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "pmflow"
