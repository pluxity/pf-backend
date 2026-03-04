rootProject.name = "pf-backend"

// ── Common Modules ──
include(":common:core")
include(":common:auth")
include(":common:file")
include(":common:messaging")
include(":common:test-support")

// ── Application Modules ──
include(":apps:safers")
include(":apps:yongin-platform")
include(":apps:weekly-report")

