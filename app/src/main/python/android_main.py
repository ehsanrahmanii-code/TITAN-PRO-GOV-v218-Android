"""Start TITAN PRO GOV v2.1.8 Flask server inside Chaquopy (Android)."""
from __future__ import annotations

import os
import sys
import threading
import time
import traceback
from pathlib import Path

_started = False
_lock = threading.Lock()
_LOG = Path("/data/data/org.titanpro.gov.v218/files/titan_boot.log")


def _log(msg: str) -> None:
    line = f"{time.strftime('%H:%M:%S')} {msg}\n"
    try:
        _LOG.parent.mkdir(parents=True, exist_ok=True)
        with _LOG.open("a", encoding="utf-8") as f:
            f.write(line)
    except Exception:
        pass
    try:
        print(line, end="", file=sys.stderr)
    except Exception:
        pass


def _prepare_home() -> Path:
    """Prefer user-visible Internal Storage/XXX; fall back to app-private."""
    candidates = [
        Path("/storage/emulated/0/XXX"),
        Path("/sdcard/XXX"),
        Path("/storage/self/primary/XXX"),
        Path("/data/data/org.titanpro.gov.v218/files/XXX"),
    ]
    for key in ("HOME", "ANDROID_PRIVATE", "TMPDIR"):
        v = (os.environ.get(key) or "").strip()
        if v:
            candidates.append(Path(v) / "XXX")
    for c in candidates:
        try:
            c.mkdir(parents=True, exist_ok=True)
            probe = c / ".titan_pro_probe"
            probe.write_text("ok", encoding="utf-8")
            probe.unlink(missing_ok=True)
            os.environ["TITAN_DATA"] = str(c)
            os.environ["TITAN_HOST"] = "127.0.0.1"
            os.environ["TITAN_PORT"] = "8080"
            secret = Path("/data/data/org.titanpro.gov.v218/files/secret")
            secret.mkdir(parents=True, exist_ok=True)
            os.environ["TITAN_SECRET"] = str(secret)
            global _LOG
            _LOG = c / "titan_boot.log"
            _log(f"home={c} secret={secret}")
            return c
        except Exception as exc:
            _log(f"home_fail {c}: {exc}")
            continue
    c = Path("/data/data/org.titanpro.gov.v218/cache/XXX")
    c.mkdir(parents=True, exist_ok=True)
    os.environ["TITAN_DATA"] = str(c)
    os.environ["TITAN_SECRET"] = str(Path("/data/data/org.titanpro.gov.v218/files/secret"))
    _log(f"home_fallback={c}")
    return c


def _run_titan_server() -> None:
    _log("import app.web.app …")
    here = Path(__file__).resolve().parent
    if str(here) not in sys.path:
        sys.path.insert(0, str(here))

    try:
        from app.web.app import app
        from app.config import VERSION, DATA_DIR
    except Exception as exc:
        _log(f"IMPORT FAIL: {type(exc).__name__}: {exc}\n{traceback.format_exc()}")
        return

    _log(f"import ok  version={VERSION} data={DATA_DIR}")
    os.environ.setdefault("TITAN_HOST", "127.0.0.1")
    os.environ.setdefault("TITAN_PORT", "8080")

    try:
        from app.engine import scanner
        if hasattr(scanner, "start_background"):
            scanner.start_background()
            _log("scanner background started")
    except Exception as e:
        _log(f"scanner start: {e}")

    try:
        from waitress import serve
        _log("serving with waitress on 127.0.0.1:8080")
        serve(app, host="127.0.0.1", port=8080, threads=8)
    except Exception as exc:
        _log(f"waitress fail, fallback flask: {exc}")
        try:
            app.run(host="127.0.0.1", port=8080, debug=False, use_reloader=False, threaded=True)
        except Exception as exc2:
            _log(f"SERVE FAIL: {type(exc2).__name__}: {exc2}\n{traceback.format_exc()}")


def start_server() -> None:
    global _started
    with _lock:
        if _started:
            return
        _started = True

    try:
        _prepare_home()
    except Exception as exc:
        _log(f"prepare_home: {exc}\n{traceback.format_exc()}")

    threading.Thread(target=_run_titan_server, name="titan-flask", daemon=True).start()
    _log("start_server scheduled")
