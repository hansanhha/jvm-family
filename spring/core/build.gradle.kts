plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {

    implementation(libs.springframework.core)
    implementation(libs.springframework.context)
    implementation(libs.springframework.context.support)
    implementation(libs.springframework.context.indexer)
    implementation(libs.springframework.aop)
    implementation(libs.springframework.aspects)
    implementation(libs.springframework.beans)

    testImplementation(libs.jupiter)
}
