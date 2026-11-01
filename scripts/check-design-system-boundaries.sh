#!/usr/bin/env bash
# Usage: check-design-system-boundaries.sh [source-root ...]
# Test sources and the design system are excluded. Exemptions require a reason.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -gt 0 ]; then roots=("$@"); else roots=(shared/src androidApp/src); fi
existing=()
for root in "${roots[@]}"; do [ ! -d "$root" ] || existing+=("$root"); done
if [ "${#existing[@]}" -eq 0 ]; then echo 'No source roots found' >&2; exit 2; fi
violations=$(
  find "${existing[@]}" -name '*.kt' -not -path '*/core/designsystem/*' -not -path '*/src/*[Tt]est*/*' -print0 |
    xargs -0 -r awk '
      function report(rule) {
        printf "%s:%d: %s — use the design system\n    %s\n", FILENAME, FNR, rule, $0
        found = 1
      }
      FNR == 1 { exempt_next = 0 }
      {
        line = $0
        trimmed = line
        sub(/^[ \t]+/, "", trimmed)
        has_exempt = (line ~ /design-system-exempt:[ \t]*[^ \t]/)
        if (trimmed ~ /^(\/\/|\/\*|\*)/) { exempt_next = has_exempt; next }
        code = line
        sub(/\/\/.*$/, "", code)
        skip = has_exempt || exempt_next
        exempt_next = 0
        if (skip) next
        if (code ~ /(^|[^[:alnum:]_])OutlinedTextField\(/) report("OutlinedTextField(")
        if (code ~ /(^|[^[:alnum:]_])Button\(/) report("Button(")
        if (code ~ /(^|[^[:alnum:]_])Color\(0x/) report("Color(0x")
        if (code ~ /(^|[^[:alnum:]_])RoundedCornerShape\(/) report("RoundedCornerShape(")
      }
      END { if (found) exit 1 }
    ' 2>&1
) && status=0 || status=$?
if [ "$status" -ne 0 ] || [ -n "$violations" ]; then
  printf '%s\n' "$violations" >&2
  echo 'Design-system boundary check failed. See AGENTS.md; exceptions require // design-system-exempt: <reason>.' >&2
  exit 1
fi
echo 'Design-system boundary check passed.'
