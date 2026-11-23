#!/usr/bin/env python3
"""MiMetrolist: abre la app en modo avión y comprueba que se vea el inicio sin conexión.

Uso: android_offline_test.py <paquete> <carpeta-salida>
"""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import android_download_test as t  # noqa: E402  (shares adb, capturas y lectura de pantalla)

PACKAGE = t.PACKAGE
OUT = t.OUT


def result(outcome, detail):
    lines = [f"resultado={outcome}", f"detalle={detail}", *t.report]
    if outcome != "OK" and t.last_root is not None:
        visible = [t.label(n) for n in t.nodes(t.last_root) if t.label(n)]
        lines.append("pantalla=" + " | ".join(visible[:60]))
    (OUT / "resultado.txt").write_text("\n".join(lines) + "\n")
    print("\n".join(lines))
    t.adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
    sys.exit(0 if outcome == "OK" else 1)


def main():
    t.adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(12)
    t.dismiss_dialogs()
    t.adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    time.sleep(5)
    t.adb("shell", "am", "force-stop", PACKAGE)
    t.adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    for _ in range(6):
        time.sleep(5)
        root = t.snapshot("sin-conexion")
        if t.find(root, "estás sin conexión"):
            library = t.find(root, "biblioteca", "library", exact=True)
            if library is None:
                result("ERROR", "Apareció el aviso, pero no la barra de navegación")
            t.tap(library, "ir a la biblioteca")
            t.snapshot("biblioteca-sin-conexion")
            result("OK", "Abrió sin internet con el inicio sin conexión y la biblioteca a mano")
    result("ERROR", "La app no mostró el inicio sin conexión en 30 segundos")


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as error:
        t.report.append(f"excepcion={type(error).__name__}: {error}")
        result("ERROR", "La prueba se detuvo con un error")
