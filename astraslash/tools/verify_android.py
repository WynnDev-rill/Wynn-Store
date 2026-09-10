"""Black-box APK installation, navigation, touch input, background and relaunch.
Pixel checks exercise real rendered screens, independently of the editor QA bridge.
Run with a booted landscape-capable emulator and Pillow installed.
"""
import io
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'evidence/android'
OUT.mkdir(parents=True, exist_ok=True)
PACKAGE = 'id.wynn.astraslash'
events = []
width, height = 1280, 720


def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=90)


def adb_try(*args):
    return subprocess.run(['adb', *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=90, check=False)


def capture(name):
    global width, height
    image = Image.open(io.BytesIO(adb('exec-out', 'screencap', '-p'))).convert('RGB')
    width, height = image.size
    image.save(OUT / (name + '.png'))
    assert width > height, 'Game did not enter landscape'
    assert max(ImageStat.Stat(image).stddev) > 18, 'Blank rendered screen'
    pid = adb('shell', 'pidof', PACKAGE).decode().strip()
    assert pid, 'Game process is no longer alive'
    events.append({'screen': name, 'width': width, 'height': height, 'process': pid})
    return image


def tap(x, y, wait=2):
    adb('shell', 'input', 'tap', str(round(x / 1280 * width)), str(round(y / 720 * height)))
    time.sleep(wait)


def cyan_at(image, x, y):
    # Sample inside the cyan primary button, away from its label.
    r, g, b = image.getpixel((round(x / 1280 * width), round(y / 720 * height)))
    return g > 125 and b > 125 and g > r * 1.25


def dismiss_android_education():
    """Dismiss Android's one-time immersive/fullscreen education when present.

    API 35 exposes this window from package ``android`` rather than
    ``com.android.systemui`` on some emulator images, so key off the stable
    android:id/ok resource and visible label instead of a package name.
    """
    dump = adb_try('shell', 'uiautomator', 'dump', '/sdcard/astraslash-window.xml')
    if dump.returncode != 0:
        return False
    read = adb_try('shell', 'cat', '/sdcard/astraslash-window.xml')
    if read.returncode != 0:
        return False
    xml = read.stdout.decode(errors='replace')
    (OUT / 'launch-window.xml').write_text(xml)
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return False
    for node in root.iter('node'):
        text = node.get('text', '').strip().lower()
        resource_id = node.get('resource-id', '')
        if resource_id == 'android:id/ok' or text in {'got it', 'ok'}:
            bounds = list(map(int, re.findall(r'-?\d+', node.get('bounds', ''))))
            if len(bounds) == 4 and node.get('clickable') == 'true':
                adb('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
                time.sleep(1)
                events.append({'system_dialog': 'fullscreen education', 'action': node.get('text', 'OK')})
                return True
    return False


def launch():
    adb('shell', 'monkey', '-p', PACKAGE, '-c', 'android.intent.category.LAUNCHER', '1')
    time.sleep(5)
    # Ask Android not to show the immersive-mode education. This setting is
    # best-effort; the UI-based dismissal below remains the compatibility path.
    adb_try('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed')
    dismiss_android_education()
    # Software Vulkan can need longer to compile shaders on a cold launch.
    deadline = time.monotonic() + 60
    while True:
        dismiss_android_education()
        title = capture('launch-wait')
        if cyan_at(title, 85, 530):
            break
        assert time.monotonic() < deadline, 'Title primary action missing after launch'
        time.sleep(2)


try:
    adb('wait-for-device')
    adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
    adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
    adb('shell', 'wm', 'size', '720x1280')
    adb_try('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed')
    adb('logcat', '-c')
    install = adb('install', '-r', str(Path(sys.argv[1]).resolve())).decode()
    assert 'Success' in install, install
    (OUT / 'install.txt').write_text(install)
    launch()
    title = capture('01-title')
    assert cyan_at(title, 85, 530), 'Title primary action missing'
    tap(230, 540, 5)
    hub = capture('02-hub')
    assert cyan_at(hub, 750, 615), 'Hub primary action missing'
    tap(1100, 112)
    capture('03-kael')
    tap(972, 625, 4)
    capture('04-story')
    tap(1171, 31, 3)
    capture('05-combat')
    tap(1137, 597, .3)
    tap(1172, 476, .3)
    tap(1004, 628, .3)
    adb('shell', 'input', 'swipe', '140', str(round(height * .82)), '250', str(round(height * .82)), '800')
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(2)
    pause = capture('06-pause')
    assert cyan_at(pause, 75, 235), 'Android Back did not open pause'
    tap(180, 315)
    capture('07-settings')
    tap(80, 247)
    capture('08-low-preset')
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(2)
    tap(170, 450)
    saved = capture('09-saved-hub')
    assert cyan_at(saved, 750, 615), 'Save and return to hub failed'
    adb('shell', 'input', 'keyevent', '3')
    time.sleep(2)
    adb('shell', 'am', 'force-stop', PACKAGE)
    launch()
    capture('10-relaunch')
    tap(230, 540, 4)
    capture('11-continue-hub')
    tap(972, 625, 4)
    capture('12-resumed-combat')
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(1)
    capture('13-resumed-pause')
    events.append({'result': 'pass', 'apk': Path(sys.argv[1]).name})
finally:
    log_result = adb_try('logcat', '-d')
    logs = log_result.stdout.decode(errors='replace')
    (OUT / 'logcat.txt').write_text(logs)
    (OUT / 'journey.json').write_text(json.dumps(events, indent=2) + '\n')
    fatal = re.findall(r'.*(?:FATAL EXCEPTION|SCRIPT ERROR:|Fatal signal).*', logs)
    if fatal:
        raise RuntimeError('\n'.join(fatal))
