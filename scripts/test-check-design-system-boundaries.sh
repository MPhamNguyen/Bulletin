#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
check=scripts/check-design-system-boundaries.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
write() { mkdir -p "$(dirname "$tmp/$1")"; cat > "$tmp/$1"; }
expect_fail() { if "$check" "$tmp/src" >/dev/null 2>&1; then echo "FAIL: $1 should fail" >&2; exit 1; fi; }
expect_pass() { if ! "$check" "$tmp/src" >/dev/null 2>&1; then echo "FAIL: $1 should pass" >&2; exit 1; fi; }
reset() { rm -rf "$tmp/src"; }
for snippet in 'OutlinedTextField(value = "", onValueChange = {})' 'Button(onClick = {}) {}' \
  'val c = Color(0xFF112233)' 'val c = androidx.compose.ui.graphics.Color(0xFF112233)' \
  'val s = RoundedCornerShape(8.dp)' 'material3.Button(onClick = {}) {}'; do
  reset; printf 'fun f() { %s }\n' "$snippet" | write src/commonMain/kotlin/app/Screen.kt
  expect_fail "$snippet"
done
for snippet in 'BulletinButton(onClick = {}) {}' 'OutlinedButton(onClick = {}) {}' 'IconButton(onClick = {}) {}' \
  'BulletinTextField(value = "", onValueChange = {})' 'val c = Color.White' 'val c = Color(red = 1f, green = 0f, blue = 0f)' \
  '// Button( is fine in a comment'; do
  reset; printf 'fun f() { %s }\n' "$snippet" | write src/commonMain/kotlin/app/Screen.kt
  expect_pass "$snippet"
done
reset; write src/commonMain/kotlin/core/designsystem/Own.kt <<'KT'
val s = RoundedCornerShape(8.dp)
val c = Color(0xFF112233)
KT
expect_pass 'design system'
reset; write src/androidDeviceTest/kotlin/app/Test.kt <<'KT'
Button(onClick = {}) {}
KT
expect_pass 'test source set'
reset; write src/commonMain/kotlin/app/Screen.kt <<'KT'
// design-system-exempt: custom interaction source
Button(onClick = {}) {}
val c = Color(0xFF112233) // design-system-exempt: brand
KT
expect_pass 'documented exemptions'
reset; write src/commonMain/kotlin/app/Screen.kt <<'KT'
// design-system-exempt:
Button(onClick = {}) {}
KT
expect_fail 'empty reason'
reset; write src/commonMain/kotlin/app/Screen.kt <<'KT'
// design-system-exempt: next line only
val ok = 1
Button(onClick = {}) {}
KT
expect_fail 'nonadjacent exemption'
echo 'design-system boundary check self-test passed'
