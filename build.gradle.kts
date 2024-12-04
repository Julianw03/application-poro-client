plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.0.0-beta2"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.eclipse.jetty.websocket:websocket-api:9.4.55.v20240627")
    implementation("org.eclipse.jetty.websocket:websocket-client:9.4.55.v20240627")
    implementation("org.eclipse.jetty.websocket:websocket-server:9.4.55.v20240627")
    implementation("org.eclipse.jetty.websocket:websocket-servlet:9.4.55.v20240627")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:2.30.1")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:2.30.1")
    implementation("org.glassfish.jersey.inject:jersey-hk2:2.30.1")
    implementation("org.glassfish.jersey.core:jersey-server:2.30.1")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:2.30.1")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.iambadatplaying.Starter")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Configure the Jar task to set the main class in the manifest
tasks.getByName<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.iambadatplaying.Starter"
    }
}

tasks.getByName<JavaExec>("run") {
    args = listOf("--dev", "--debug-port=3000")
}

