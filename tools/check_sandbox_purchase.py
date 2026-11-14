"""Exercise Amazon App Tester checkout and restore on an isolated installed APK."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.skillhando.app.billingtest"


def run(adb, *args):
    return subprocess.run([*adb, *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace", check=True, timeout=30).stdout


def surface(adb):
    name = "/sdcard/skillhando-billingtest-view.xml"
    try:
        result = run(adb, "shell", "uiautomator", "dump", name)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return []
    if "dumped" not in result:
        return []
    try:
        return list(ET.fromstring(run(adb, "shell", "cat", name)).iter("node"))
    except ET.ParseError:
        return []


def wait_for(adb, text, seconds=20):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        for node in surface(adb):
            if re.search(text, node.get("text", ""), re.I):
                return node
        time.sleep(.5)
    raise RuntimeError(f"Expected screen text: {text}")


def tap(adb, node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
    run(adb, "shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def show_premium(adb):
    run(adb, "shell", "am", "start", "-W", "-n", PACKAGE+"/com.skillhando.app.MainActivity")
    matches = surface(adb)
    skip = next((n for n in matches if n.get("text") == "Skip setup"), None)
    if skip is None and any("small win" in n.get("text", "") for n in matches):
        run(adb, "shell", "input", "swipe", "1400", "1000", "1400", "320", "500")
        skip = wait_for(adb, "^Skip setup$")
    if skip is not None:
        tap(adb, skip)
    tap(adb, wait_for(adb, "^More$"))
    for _ in range(4):
        match = next((n for n in surface(adb) if n.get("text") == "View Premium"), None)
        if match is not None:
            tap(adb, match)
            wait_for(adb, "^Restore purchases$")
            return
        run(adb, "shell", "input", "swipe", "1400", "1010", "1400", "390", "450")
    raise RuntimeError("Cannot find View Premium")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--stage", choices=["open", "verify", "restore"], required=True)
    options = parser.parse_args()
    adb = [options.adb, "-s", options.serial]
    if run(adb, "shell", "getprop", "debug.amazon.sandboxmode").strip() != "debug":
        raise RuntimeError("Amazon App Tester sandbox mode is required")
    if options.stage == "open":
        show_premium(adb)
        price = wait_for(adb, r"^Buy Premium · \S", 35)
        tap(adb, price)
        item = wait_for(adb, "^Get Item$")
        if item.get("package") != "com.amazon.sdktestclient":
            raise RuntimeError("Checkout confirmation must come from Amazon App Tester")
        tap(adb, item)
        confirmation = wait_for(adb, r"^Thank you! Your order is complete\.$")
        if confirmation.get("package") != "com.amazon.sdktestclient":
            raise RuntimeError("Checkout completion must come from Amazon App Tester")
        tap(adb, wait_for(adb, "^Close$"))
        print("PASS checkout: Amazon App Tester displayed a price and completed the sandbox order", flush=True)
        return
    if options.stage == "restore":
        show_premium(adb)
        tap(adb, wait_for(adb, "^Restore purchases$"))
    wait_for(adb, "^Premium purchase verified\.$", 45)
    wait_for(adb, "^Premium is active$")
    print(f"PASS {options.stage}: server verified receipt and Premium is active")


if __name__ == "__main__":
    main()
