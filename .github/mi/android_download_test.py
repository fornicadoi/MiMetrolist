#!/usr/bin/env python3
"""MiMetrolist: prueba de descargas en el emulador.

Abre una canción con un enlace de YouTube Music, la descarga desde el menú del reproductor
y comprueba que el archivo quede guardado en la app. Deja capturas y el árbol de la
interfaz de cada paso en la carpeta de salida, y escribe resultado.txt.

Uso: android_download_test.py <paquete> <carpeta-salida>
"""

import json
import re
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = sys.argv[1]
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
QUERY = "Daft Punk Get Lucky"
BLOCK_HINTS = ("not a bot", "sign in to confirm", "login_required", "429", "too many requests", "unusual traffic")
SKIP_WORDS = ("skip", "continue", "next", "get started", "done", "allow", "ok", "got it", "not now", "accept", "agree", "close")
step = 0
report = []


def adb(*args, check=False, timeout=60):
    result = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {result.stderr.strip()}")
    return result.stdout


def snapshot(name):
    global step
    step += 1
    base = OUT / f"{step:02d}-{name}"
    with open(f"{base}.png", "wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, timeout=60)
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    adb("pull", "/sdcard/ui.xml", f"{base}.xml")
    try:
        return ET.parse(f"{base}.xml").getroot()
    except Exception:
        return None


def nodes(root):
    if root is None:
        return []
    found = []
    for node in root.iter("node"):
        bounds = re.findall(r"\d+", node.get("bounds", ""))
        if len(bounds) != 4:
            continue
        x1, y1, x2, y2 = map(int, bounds)
        found.append(
            {
                "text": (node.get("text") or "").strip(),
                "desc": (node.get("content-desc") or "").strip(),
                "clickable": node.get("clickable") == "true",
                "center": ((x1 + x2) // 2, (y1 + y2) // 2),
                "area": (x2 - x1) * (y2 - y1),
                "top": y1,
            }
        )
    return found


def label(node):
    return (node["text"] or node["desc"]).lower()


def tap(node, why):
    x, y = node["center"]
    report.append(f"toque: {why} ({label(node)!r} en {x},{y})")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)


def find(root, *words, exact=False):
    for node in nodes(root):
        text = label(node)
        if not text:
            continue
        if any(text == w if exact else w in text for w in words):
            return node
    return None


def song_id():
    """Busca la canción en YouTube Music sin cuenta, igual que una búsqueda anónima."""
    body = {
        "context": {"client": {"clientName": "WEB_REMIX", "clientVersion": "1.20241106.01.00", "hl": "en", "gl": "US"}},
        "query": QUERY,
        "params": "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D",
    }
    request = urllib.request.Request(
        "https://music.youtube.com/youtubei/v1/search?prettyPrint=false",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", "User-Agent": "Mozilla/5.0", "Origin": "https://music.youtube.com"},
    )
    data = json.loads(urllib.request.urlopen(request, timeout=30).read())

    def walk(value):
        if isinstance(value, dict):
            if "musicResponsiveListItemRenderer" in value:
                item = value["musicResponsiveListItemRenderer"]
                vid = re.search(r'"videoId": ?"([A-Za-z0-9_-]{11})"', json.dumps(item))
                runs = item.get("flexColumns", [{}])[0].get("musicResponsiveListItemFlexColumnRenderer", {}).get("text", {}).get("runs", [])
                if vid and runs:
                    return vid.group(1), runs[0].get("text", "")
            for child in value.values():
                found = walk(child)
                if found:
                    return found
        elif isinstance(value, list):
            for child in value:
                found = walk(child)
                if found:
                    return found
        return None

    return walk(data) or (None, None)


def downloaded_files():
    listing = adb("shell", f"su 0 find /data/data/{PACKAGE}/files/downloads -type f -size +100k 2>/dev/null")
    if not listing.strip():
        listing = adb("shell", f"find /data/data/{PACKAGE}/files/downloads -type f -size +100k 2>/dev/null")
    return [line for line in listing.splitlines() if line.strip()]


def logcat_text():
    return adb("logcat", "-d", "-t", "4000", timeout=120)


def finish(outcome, detail):
    log = logcat_text()
    (OUT / "logcat.txt").write_text(log)
    if outcome == "ERROR" and any(h in log.lower() for h in BLOCK_HINTS):
        outcome = "BLOQUEO"
    lines = [f"resultado={outcome}", f"detalle={detail}", *report]
    (OUT / "resultado.txt").write_text("\n".join(lines) + "\n")
    print("\n".join(lines))
    sys.exit(1 if outcome == "ERROR" else 0)


def dismiss_dialogs(rounds=6):
    for _ in range(rounds):
        root = snapshot("pantalla")
        button = None
        for node in nodes(root):
            if node["clickable"] and label(node) in SKIP_WORDS:
                button = node
                break
        if button is None:
            return root
        tap(button, "cerrar aviso")
    return snapshot("pantalla")


def main():
    vid, title = song_id()
    if not vid:
        finish("ERROR", "No se encontró una canción para probar")
    report.append(f"cancion={title} https://music.youtube.com/watch?v={vid}")
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-a", "android.intent.action.VIEW", "-d", f"https://music.youtube.com/watch?v={vid}", PACKAGE)
    time.sleep(20)
    root = dismiss_dialogs()

    # Abrir el reproductor completo si solo está el mini reproductor.
    for _ in range(3):
        menu = find(root, "more options", "menu")
        if menu and menu["top"] > 0:
            break
        player = find(root, title.lower())
        if player:
            tap(player, "abrir reproductor")
        root = snapshot("reproductor")

    menu = find(root, "more options", "menu")
    if not menu:
        finish("ERROR", "No apareció el menú del reproductor")
    tap(menu, "menú del reproductor")
    root = snapshot("menu")
    download = find(root, "download", exact=True) or find(root, "download")
    if not download:
        adb("shell", "input", "swipe", "540", "1800", "540", "900", "400")
        time.sleep(2)
        root = snapshot("menu-abajo")
        download = find(root, "download", exact=True) or find(root, "download")
    if not download:
        finish("ERROR", "No apareció la opción Download en el menú")
    tap(download, "descargar")

    for second in range(0, 240, 10):
        files = downloaded_files()
        if files:
            snapshot("descargada")
            report.append(f"tiempo={second}s")
            report.extend(f"archivo={line}" for line in files[:5])
            finish("OK", "La canción quedó descargada en la app")
        time.sleep(10)
    snapshot("sin-archivo")
    finish("ERROR", "No apareció ningún archivo descargado en 4 minutos")


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as error:
        report.append(f"excepcion={type(error).__name__}: {error}")
        finish("ERROR", "La prueba se detuvo con un error")
