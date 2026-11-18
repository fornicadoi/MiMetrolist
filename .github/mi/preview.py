#!/usr/bin/env python3
"""MiMetrolist: abre la app de escritorio en una pantalla virtual, mide el arranque
y saca capturas de las pantallas principales (inicio, biblioteca, buscador)."""

import os
import subprocess
import sys
import time
from pathlib import Path

app = Path(sys.argv[1]).resolve()
out = Path(sys.argv[2]).resolve()
out.mkdir(parents=True, exist_ok=True)
profile = out.parent / "preview-profile"
WIDTH, HEIGHT = 1366, 768
timings = []


def log(message):
    print(message, flush=True)
    timings.append(message)


def env():
    e = dict(os.environ)
    for kind in ("CONFIG", "DATA", "CACHE", "STATE"):
        d = profile / kind.lower()
        d.mkdir(parents=True, exist_ok=True)
        e[f"XDG_{kind}_HOME"] = str(d)
    e["HOME"] = str(profile)
    # Salta el asistente de bienvenida para llegar directo al inicio.
    settings = profile / "config" / "metrolist" / "settings.properties"
    if not settings.exists():
        settings.parent.mkdir(parents=True, exist_ok=True)
        settings.write_text("desktopOnboardingInitialized=true\ndesktopOnboardingComplete=true\n", encoding="utf-8")
    e["APPIMAGE_EXTRACT_AND_RUN"] = "1"
    e["LIBGL_ALWAYS_SOFTWARE"] = "1"
    e["SKIKO_RENDER_API"] = "SOFTWARE"
    e["SKIKO_WAYLAND"] = "0"
    e["_JAVA_OPTIONS"] = f"-Duser.home={profile} -Duser.language=es -Duser.country=DO"
    return e


def sh(*args, check=False):
    return subprocess.run(list(args), capture_output=True, text=True, check=check)


def shot(name):
    path = out / f"{name}.png"
    sh("import", "-window", "root", str(path))
    return path


def window_id():
    for name in ("Metrolist", "MiMetrolist"):
        r = sh("xdotool", "search", "--onlyvisible", "--name", name)
        ids = [line for line in r.stdout.split() if line.strip()]
        if ids:
            return ids[-1]
    return None


def key(*keys):
    sh("xdotool", "key", "--clearmodifiers", *keys)


def click(x, y):
    sh("xdotool", "mousemove", str(x), str(y), "click", "1")


def launch(label):
    start = time.monotonic()
    proc = subprocess.Popen([str(app)], env=env(), stdout=open(out / f"{label}-stdout.log", "w"), stderr=subprocess.STDOUT)
    wid = None
    while time.monotonic() - start < 120:
        if proc.poll() is not None:
            log(f"[{label}] la app se cerró durante el arranque (código {proc.returncode})")
            return proc, None
        wid = window_id()
        if wid:
            break
        time.sleep(0.25)
    if not wid:
        log(f"[{label}] no apareció ninguna ventana en 120 s")
        return proc, None
    log(f"[{label}] ventana visible a los {time.monotonic() - start:.1f} s")
    sh("xdotool", "windowsize", wid, str(WIDTH), str(HEIGHT))
    sh("xdotool", "windowmove", wid, "0", "0")
    # Capturas durante la carga para ver cuándo aparece el contenido del inicio.
    for second in (2, 5, 10, 20, 30):
        while time.monotonic() - start < second + 1:
            time.sleep(0.2)
        shot(f"{label}-t{second:02d}s")
    return proc, wid


def close(proc, label):
    key("alt+F4")
    try:
        proc.wait(timeout=30)
        log(f"[{label}] cerrada normalmente (código {proc.returncode})")
    except subprocess.TimeoutExpired:
        proc.terminate()
        try:
            proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            proc.kill()
        log(f"[{label}] no se cerró con Alt+F4; se terminó el proceso")


# Primer arranque: perfil vacío, sin caché de arranque.
proc, wid = launch("frio")
if wid:
    time.sleep(10)
    shot("01-inicio")
    sh("xdotool", "mousemove", str(WIDTH // 2), str(HEIGHT // 2))
    for _ in range(6):
        sh("xdotool", "click", "5")
    time.sleep(2)
    shot("02-inicio-abajo")
    # Barra lateral: Inicio, Biblioteca, Historial, Estadísticas (como en la captura del usuario).
    click(37, 240)
    time.sleep(4)
    shot("03-biblioteca")
    key("ctrl+k")
    time.sleep(2)
    shot("04-buscador")
    sh("xdotool", "type", "--delay", "60", "salsa")
    time.sleep(1)
    key("Return")
    time.sleep(8)
    shot("05-buscador-resultados")
    key("Escape")
    time.sleep(1)
    click(37, 172)
    time.sleep(3)
close(proc, "frio")

# Segundo arranque: ya con la caché creada al cerrar.
time.sleep(3)
proc, wid = launch("tibio")
close(proc, "tibio")

(out / "tiempos.txt").write_text("\n".join(timings) + "\n", encoding="utf-8")
# Diagnóstico: qué archivos de ajustes creó la app.
listing = sh("find", str(profile), "-maxdepth", "4", "-type", "f").stdout
(out / "perfil.txt").write_text(listing, encoding="utf-8")
for found in profile.rglob("settings.properties"):
    (out / ("ajustes-" + found.parent.name + ".txt")).write_text(found.read_text(errors="replace"), encoding="utf-8")
