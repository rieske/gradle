plugins {
    id("myproject.java-library-conventions")
}

// tag::dependencies[]
dependencies {
    // This is a platform dependency but you want the library
    implementation(platform("com.google.guava:guava:28.2-jre"))
}
// end::dependencies[]
