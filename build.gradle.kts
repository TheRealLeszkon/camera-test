// Root build file. Plugins are declared here (with `apply false`) so that every
// module resolves the same plugin versions, then applied in the module that needs them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
