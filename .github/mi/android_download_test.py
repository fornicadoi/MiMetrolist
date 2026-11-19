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
BLOCK_HINTS = ("not a bot", "sign in to confirm", "login_required", "http 429", "status code 429", "too many requests", "unusual traffic")
# Botones de la bienvenida y de avisos, en orden de preferencia ("No" rechaza los reportes de errores).
SKIP_WORDS = (
    "begin setup",
    "continue with an empty library",
    "no",
    "continue",
    "finish",
    "allow",
    "skip",
    "not now",
    "got it",
    "ok",
    "close",
)
step = 0
report = []
last_root = None


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
    global last_root
    try:
        last_root = ET.parse(f"{base}.xml").getroot()
    except Exception:
        last_root = None
    return last_root


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
    pids = adb("shell", "pidof", PACKAGE).split()
    app = "".join(adb("logcat", "-d", f"--pid={pid}", timeout=120) for pid in pids)
    (OUT / "logcat-app.txt").write_text(app)
    return adb("logcat", "-d", timeout=120)


def finish(outcome, detail):
    log = logcat_text()
    (OUT / "logcat.txt").write_text(log)
    if outcome == "ERROR" and any(h in log.lower() for h in BLOCK_HINTS):
        outcome = "BLOQUEO"
    app_log = (OUT / "logcat-app.txt").read_text() if (OUT / "logcat-app.txt").exists() else ""
    interesting = [line[33:].strip() for line in app_log.splitlines() if " E " in line[:40] or "download" in line.lower() or "stream" in line.lower()]
    report.extend(f"log={line[:300]}" for line in interesting[-30:])
    hints = sorted({h for h in BLOCK_HINTS if h in log.lower()})
    if hints:
        report.append("pistas=" + ", ".join(hints))
    if outcome != "OK" and last_root is not None:
        visible = [f"{label(n)}{'*' if n['clickable'] else ''}@{n['center'][0]},{n['center'][1]}" for n in nodes(last_root) if label(n)]
        report.append("pantalla=" + " | ".join(visible[:60]))
    lines = [f"resultado={outcome}", f"detalle={detail}", *report]
    (OUT / "resultado.txt").write_text("\n".join(lines) + "\n")
    print("\n".join(lines))
    sys.exit(1 if outcome == "ERROR" else 0)


def dismiss_dialogs(rounds=14):
    for _ in range(rounds):
        root = snapshot("pantalla")
        labels = {label(node): node for node in nodes(root) if label(node)}
        word = next((w for w in SKIP_WORDS if w in labels), None)
        if word is None:
            return root
        tap(labels[word], "bienvenida o aviso")
    return snapshot("pantalla")


def main():
    vid, title = song_id()
    if not vid:
        finish("ERROR", "No se encontró una canción para probar")
    report.append(f"cancion={title} https://music.youtube.com/watch?v={vid}")
    adb("logcat", "-G", "16M")
    adb("logcat", "-c")
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(15)
    dismiss_dialogs()
    adb("shell", "am", "start", "-W", "-a", "android.intent.action.VIEW", "-d", f"https://music.youtube.com/watch?v={vid}", PACKAGE)
    time.sleep(20)
    root = dismiss_dialogs(4)

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
    size = re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    width, height = map(int, size[-1]) if size else (1080, 2400)
    download = find(root, "download", exact=True)
    for _ in range(4):
        if download:
            break
        x = str(width // 2)
        adb("shell", "input", "swipe", x, str(height * 85 // 100), x, str(height * 45 // 100), "500")
        time.sleep(2)
        root = snapshot("menu-abajo")
        download = find(root, "download", exact=True)
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
    report.append("archivos-app=" + adb("shell", f"find /data/data/{PACKAGE}/files -maxdepth 2 -type d").replace("\n", " "))
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
