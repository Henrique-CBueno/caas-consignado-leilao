#!/bin/sh
# Auto-teste do verificador: monta árvores de docs mínimas em diretório temporário,
# introduz um defeito por vez e exige que docs-check.sh o detecte (ou passe, na árvore boa).
HERE=$(cd "$(dirname "$0")" && pwd)
CHECK="$HERE/docs-check.sh"
FAILS=0

mkroot() {
  R=$(mktemp -d)
  mkdir -p "$R/docs/adr"
  printf 'test:\n\techo ok\n' > "$R/Makefile"
  printf '# Projeto\nVeja [ADRs](docs/adr/README.md). Rode `make test`.\n' > "$R/README.md"
  printf '# ADRs\n- [0001](0001-exemplo.md)\n' > "$R/docs/adr/README.md"
  printf '# 0001\n\n## Status\nAceita\n\n## Contexto\nx\n\n## Decisão\nx\n\n## Consequências\nx\n' > "$R/docs/adr/0001-exemplo.md"
  echo "$R"
}

expect_pass() {
  if sh "$CHECK" "$2" >/dev/null 2>&1; then echo "ok   - $1"; else echo "FAIL - $1 (esperava passar)"; FAILS=$((FAILS+1)); fi
}
expect_fail() {
  OUT=$(sh "$CHECK" "$2" 2>&1) && { echo "FAIL - $1 (esperava falhar)"; FAILS=$((FAILS+1)); return; }
  case "$OUT" in *"$3"*) echo "ok   - $1" ;; *) echo "FAIL - $1 (mensagem sem '$3'): $OUT"; FAILS=$((FAILS+1)) ;; esac
}

R=$(mkroot);                                                       expect_pass "árvore boa passa" "$R"
R=$(mkroot); printf 'Veja [x](docs/nao-existe.md)\n' >> "$R/README.md"
                                                                   expect_fail "link relativo quebrado" "$R" "link quebrado"
R=$(mkroot); printf '# 0002\n\n## Status\nAceita\n\n## Contexto\nx\n\n## Decisão\nx\n' > "$R/docs/adr/0002-sem-consequencias.md"
printf -- '- [0002](0002-sem-consequencias.md)\n' >> "$R/docs/adr/README.md"
                                                                   expect_fail "ADR sem seção obrigatória" "$R" "seção ausente"
R=$(mkroot); cp "$R/docs/adr/0001-exemplo.md" "$R/docs/adr/0002-fora-do-indice.md"
                                                                   expect_fail "ADR fora do índice" "$R" "fora do índice"
R=$(mkroot); printf 'Rode `make inexistente`.\n' >> "$R/README.md"
                                                                   expect_fail "alvo make inexistente" "$R" "alvo make inexistente"

[ "$FAILS" -eq 0 ] && echo "docs-check.test: tudo ok" || { echo "docs-check.test: $FAILS falha(s)"; exit 1; }
