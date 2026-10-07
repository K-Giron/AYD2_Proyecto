#!/bin/sh
# Regenera los PNG/SVG de los diagramas NUEVOS de Fase 2 (requiere Docker).
cd "$(dirname "$0")/nuevos-fase2" && docker run --rm -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" -v "$PWD":/d -w /d alpine:3.20 sh -c \
  'apk add --no-cache graphviz ttf-dejavu >/dev/null 2>&1; for f in *.dot; do dot -Tpng -Gdpi=130 "$f" -o "${f%.dot}.png"; dot -Tsvg "$f" -o "${f%.dot}.svg"; done; chown "$HOST_UID:$HOST_GID" *.png *.svg'
