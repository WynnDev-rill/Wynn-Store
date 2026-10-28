"""Exercise the real device-QA exit status with controlled adb output."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


RUNNER = Path(__file__).with_name("run-device.sh").resolve()


class DeviceQaOutcomeTest(unittest.TestCase):
    def test_reports_and_crashes_determine_success(self):
        def crash(package):
            return f"FATAL EXCEPTION: main\nProcess: {package}, PID: 1\nexception\nstack\n"

        cases = [
            ("zero tests", "OK (0 tests)", "", 1),
            ("six tests", "OK (6 tests)", "", 0),
            ("one test", "OK (1 test)", "", 0),
            ("failed test", "FAILURES!!!\nTests run: 6, Failures: 1", "", 1),
            ("app crash", "OK (6 tests)", crash("id.wynn.iwatchyou"), 1),
            ("fixture crash", "OK (6 tests)", crash("id.wynn.iwatchyou.test"), 1),
            ("unrelated crash", "OK (6 tests)", crash("unrelated.app"), 0),
            ("large crash log", "OK (6 tests)", crash("id.wynn.iwatchyou.test") * 10_000, 1),
        ]
        for name, report, log, expected in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory(prefix="iwatchyou-qa-") as directory:
                root = Path(directory)
                (root / "iwatchyou/app/build").mkdir(parents=True)
                (root / "bin").mkdir()
                log_file = root / "fixture-log.txt"
                log_file.write_text(log)
                adb = root / "bin/adb"
                adb.write_text(
                    "#!/usr/bin/env python3\n"
                    "import os, pathlib, sys\n"
                    "if 'instrument' in sys.argv:\n"
                    "    print(os.environ['IW_QA_REPORT'])\n"
                    "elif sys.argv[1:] == ['logcat', '-d']:\n"
                    "    print(pathlib.Path(os.environ['IW_QA_LOGFILE']).read_text())\n"
                )
                adb.chmod(0o755)
                env = dict(
                    os.environ,
                    PATH=str(root / "bin") + os.pathsep + os.environ["PATH"],
                    IW_QA_REPORT=report,
                    IW_QA_LOGFILE=str(log_file),
                )
                result = subprocess.run(
                    ["bash", str(RUNNER)], cwd=root, env=env, capture_output=True, text=True
                )
                self.assertEqual(expected, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
