#!/bin/sh
# Verifica a integridade da documentação. Uso: docs-check.sh [RAIZ] (padrão: diretório atual).
ROOT=${1:-.}
cd "$ROOT" || exit 2
ERRORS=0
fail() { echo "docs-check: $1"; ERRORS=$((ERRORS+1)); }

DOCS=$(ls README.md CLAUDE.md CONTEXT.md 2>/dev/null; find docs -name '*.md' 2>/dev/null)

# 1. Links relativos entre documentos precisam apontar para arquivos existentes.
for f in $DOCS; do
  dir=$(dirname "$f")
  grep -o '\]([^)]*)' "$f" | sed 's/^](//; s/)$//' | while read -r target; do
    case "$target" in http://*|https://*|mailto:*|\#*|"") continue ;; esac
    path=${target%%#*}
    [ -e "$dir/$path" ] || echo "docs-check: link quebrado em $f: $target"
  done
done > /tmp/docs-check-links.$$
if [ -s /tmp/docs-check-links.$$ ]; then cat /tmp/docs-check-links.$$; ERRORS=$((ERRORS+$(wc -l < /tmp/docs-check-links.$$))); fi
rm -f /tmp/docs-check-links.$$

# 2. Todo ADR tem as seções obrigatórias e consta no índice (docs/adr/README.md).
for adr in $(find docs/adr -name '[0-9]*.md' 2>/dev/null | sort); do
  for section in "Status" "Contexto" "Decisão" "Consequências"; do
    grep -q "^## $section" "$adr" || fail "seção ausente em $adr: $section"
  done
  grep -q "$(basename "$adr")" docs/adr/README.md 2>/dev/null || fail "ADR fora do índice: $adr"
done

# 3. Todo `make <alvo>` citado em código (inline ou em bloco) existe no Makefile.
TARGETS=$(grep -oE '^[a-zA-Z0-9_.-]+:' Makefile 2>/dev/null | tr -d ':')
for f in $DOCS; do
  awk '
    /^```/ { fence = !fence; next }
    fence { if (match($0, /^[ \t$]*make[ \t]+[a-z][a-z0-9-]*/)) print substr($0, RSTART, RLENGTH); next }
    { line = $0
      while (match(line, /`make[ \t]+[a-z][a-z0-9-]*/)) { print substr(line, RSTART + 1, RLENGTH - 1); line = substr(line, RSTART + RLENGTH) } }
  ' "$f" | awk '{ print $NF }' | sort -u | while read -r t; do
    echo "$TARGETS" | grep -qx "$t" || echo "docs-check: alvo make inexistente em $f: make $t"
  done
done > /tmp/docs-check-make.$$
if [ -s /tmp/docs-check-make.$$ ]; then cat /tmp/docs-check-make.$$; ERRORS=$((ERRORS+$(wc -l < /tmp/docs-check-make.$$))); fi
rm -f /tmp/docs-check-make.$$

[ "$ERRORS" -eq 0 ] && echo "docs-check: ok" || { echo "docs-check: $ERRORS problema(s)"; exit 1; }
