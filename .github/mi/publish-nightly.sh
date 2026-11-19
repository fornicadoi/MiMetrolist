#!/usr/bin/env bash
# Sube un archivo a la versión "nightly" de MiMetrolist (la crea si no existe).
# La app de Android y la de Windows buscan sus actualizaciones en esta versión.
# Las notas listan los cambios desde la versión anterior, y la app las muestra como "Novedades".
set -euo pipefail
file="$1"
repo="$GITHUB_REPOSITORY"
upstream=$(cut -c1-7 .github/mi/upstream-sha 2>/dev/null || echo "?")
title="MiMetrolist ${APP_VERSION} ($(date -u +%Y-%m-%d))"

# Convierte "feat(mio): algo" en "Algo", "fix(mio): algo" en "Arreglo: algo" y salta los cambios internos.
changes_since() {
  git log --no-merges --format='%s' "$1..HEAD" 2>/dev/null | while IFS= read -r subject; do
    case "$subject" in
      ci*|docs*|chore*|style*|test*) continue ;;
      sync:*) echo "- Actualizado a Metrolist ${subject#sync: Metrolist }" ;;
      fix*:*) text="${subject#*: }"; echo "- Arreglo: ${text}" ;;
      *:*) text="${subject#*: }"; echo "- ${text^}" ;;
      *) echo "- ${subject^}" ;;
    esac
  done | head -n 12
}

if gh release view nightly -R "$repo" >/dev/null 2>&1; then
  previous=$(gh release view nightly -R "$repo" --json targetCommitish -q .targetCommitish || true)
  # El primero que publica esta versión escribe las notas; los demás solo suben su archivo.
  if [ "$previous" != "$GITHUB_SHA" ]; then
    changes=""
    if [ -n "$previous" ] && git cat-file -e "${previous}^{commit}" 2>/dev/null; then
      changes=$(changes_since "$previous")
    fi
    [ -n "$changes" ] || changes="- Mejoras y arreglos"
    notes="## Novedades
${changes}

## Instalar
- Android: actualiza desde el aviso de la app, o descarga MiMetrolist.apk e instálala encima de la anterior.
- Windows: descarga MiMetrolist-windows.zip, descomprímelo y abre Metrolist.exe.
- Windows nativo (experimental): MiMetrolist-windows-nativo.zip, llega un rato después.

Basada en Metrolist ${upstream}."
    gh release edit nightly -R "$repo" --target "$GITHUB_SHA" --title "$title" --notes "$notes" --prerelease
  fi
else
  gh release create nightly -R "$repo" --target "$GITHUB_SHA" --prerelease --title "$title" --notes "## Novedades
- Primera versión de MiMetrolist"
fi
gh release upload nightly "$file" -R "$repo" --clobber
