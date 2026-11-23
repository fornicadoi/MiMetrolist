#!/usr/bin/env bash
# MiMetrolist: corre las pruebas del emulador una tras otra y falla si alguna falla.
set -u
python3 .github/mi/android_download_test.py "$1" "$2/android"
download=$?
python3 .github/mi/android_offline_test.py "$1" "$2/android/sin-conexion"
offline=$?
exit $(( download != 0 || offline != 0 ))
