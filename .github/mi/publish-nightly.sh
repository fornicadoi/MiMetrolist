#!/usr/bin/env bash
# Sube un archivo a la versión "nightly" de MiMetrolist (la crea si no existe).
# La app de Android y la de Windows buscan sus actualizaciones en esta versión.
set -euo pipefail
file="$1"
repo="$GITHUB_REPOSITORY"
upstream=$(cut -c1-7 .github/mi/upstream-sha 2>/dev/null || echo "?")
title="MiMetrolist ${APP_VERSION} ($(date -u +%Y-%m-%d))"
notes="Basada en la nightly de Metrolist ${upstream}.

- Android: descarga MiMetrolist.apk e instálala encima de la anterior, o actualiza desde Ajustes > Actualizaciones.
- Windows: descarga MiMetrolist-windows.zip, descomprímelo y abre Metrolist.exe.
- Windows nativo (experimental, abre más rápido): MiMetrolist-windows-nativo.zip. Llega un rato después que las demás."
if gh release view nightly -R "$repo" >/dev/null 2>&1; then
  gh release edit nightly -R "$repo" --target "$GITHUB_SHA" --title "$title" --notes "$notes" --prerelease
else
  gh release create nightly -R "$repo" --target "$GITHUB_SHA" --prerelease --title "$title" --notes "$notes"
fi
gh release upload nightly "$file" -R "$repo" --clobber
