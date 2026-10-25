"""Compare real host builds; requires Python 3.11+ and psutil.

Cold means cleaned module outputs with build/configuration caches disabled.
Warm retains compiled outputs but reruns both test tasks (never a no-op sample).
Dependency downloads and OS caches are prewarmed, not purged. Each invocation
uses a fresh Gradle daemon and an isolated Kotlin daemon registry, retaining
default compiler execution and heap settings while excluding others' daemons.
"""

import argparse
import asyncio
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
import time
import xml.etree.ElementTree as ET

import psutil


ROOT = Path(__file__).resolve().parents[1]
MODULES = ("ext-func-compiler", "ui-list-annotation-compiler-ksp")
DEPENDENCIES = ("ext-func-definition", "ui-list-annotation-common", "ui-list-annotation-definition")


async def invoke(arguments, log_path):
    wrapper = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    command = (["cmd.exe", "/d", "/c", wrapper] if os.name == "nt" else [wrapper])
    command += arguments
    registry = tempfile.TemporaryDirectory(prefix="common-ui-gradle-benchmark-")
    command += [f"-Dkotlin.daemon.options=runFilesPath={Path(registry.name).as_posix()}"]
    started = time.perf_counter()
    peak_rss = peak_private = 0
    min_available = psutil.virtual_memory().available
    known = set()
    kotlin_daemon_seen = False
    try:
        with log_path.open("w", encoding="utf-8") as log:
            process = await asyncio.create_subprocess_exec(
                *command, cwd=ROOT, stdout=log, stderr=asyncio.subprocess.STDOUT
            )
            known.add(psutil.Process(process.pid))
            while process.returncode is None:
                rss = private = 0
                for parent in tuple(known):
                    try:
                        known.update(parent.children(recursive=True))
                    except psutil.NoSuchProcess:
                        pass
                for child in tuple(known):
                    try:
                        memory = child.memory_info()
                        rss += memory.rss
                        private += (memory.private if hasattr(memory, "private")
                                    else child.memory_full_info().uss)
                        kotlin_daemon_seen |= any("KotlinCompileDaemon" in arg
                                                  for arg in child.cmdline())
                    except psutil.NoSuchProcess:
                        known.discard(child)
                peak_rss = max(peak_rss, rss)
                peak_private = max(peak_private, private)
                min_available = min(min_available, psutil.virtual_memory().available)
                await asyncio.sleep(0.2)
            code = await process.wait()
        elapsed = time.perf_counter() - started
    finally:
        # The isolated registry prevents sharing these compiler daemons. Clean
        # them up after the timed invocation, and on cancellation or failure.
        for child in known:
            try:
                child.terminate()
            except psutil.NoSuchProcess:
                pass
        _, alive = await asyncio.to_thread(psutil.wait_procs, known, timeout=5)
        for child in alive:
            try:
                child.kill()
            except psutil.NoSuchProcess:
                pass
        await asyncio.to_thread(psutil.wait_procs, alive, timeout=5)
        registry.cleanup()
    result = {
        "exit_code": code, "seconds": round(elapsed, 3),
        "kotlin_daemon_seen": kotlin_daemon_seen,
        "peak_tree_rss_mib": round(peak_rss / 2**20, 1),
        "peak_tree_private_mib": round(peak_private / 2**20, 1),
        "min_system_available_mib": round(min_available / 2**20, 1),
    }
    if code:
        raise RuntimeError(f"Build failed ({code}); see {log_path}")
    return result


def evidence(log_path):
    intervals = {}
    for task, event, millis in re.findall(
        r"BENCH_TASK\|([^|]+)\|(start|end)\|(\d+)", log_path.read_text(encoding="utf-8")
    ):
        intervals.setdefault(task, {})[event] = int(millis)
    expected = [f":{module}:test" for module in MODULES]
    if any(set(intervals.get(task, {})) != {"start", "end"} for task in expected):
        raise RuntimeError("Missing executed test intervals; cached/no-op tests are not valid samples")
    overlap = max(0, min(intervals[t]["end"] for t in expected)
                  - max(intervals[t]["start"] for t in expected))
    suites = []
    for module in MODULES:
        files = list((ROOT / module / "build/test-results/test").glob("TEST-*.xml"))
        if not files:
            raise RuntimeError(f"Missing test XML for {module}")
        for file in files:
            suite = ET.parse(file).getroot()
            if int(suite.get("failures", 0)) or int(suite.get("errors", 0)):
                raise RuntimeError(f"Test failure in {file}")
            suites.append({"module": module, "name": suite.get("name"),
                           "tests": int(suite.get("tests", 0)),
                           "skipped": int(suite.get("skipped", 0))})
    return {"test_intervals_ms": intervals, "test_overlap_seconds": overlap / 1000,
            "test_suites": suites}


async def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repeats", type=int, default=3)
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--output", type=Path, default=ROOT / "build/parallel-benchmark")
    args = parser.parse_args()
    if args.repeats < 1 or args.workers < 2:
        parser.error("repeats must be positive and workers must be at least 2")
    args.output.mkdir(parents=True, exist_ok=True)
    common = ["--no-daemon", "--no-configuration-cache", "--no-build-cache",
              f"--max-workers={args.workers}", "--console=plain"]
    tests = [item for module in MODULES for item in (f":{module}:test", "--rerun")]
    results = []
    for repeat in range(1, args.repeats + 1):
        # Alternate order to reduce systematic bias from machine load.
        modes = (False, True) if repeat % 2 else (True, False)
        for parallel in modes:
            mode = "parallel" if parallel else "serial"
            flag = "--parallel" if parallel else "--no-parallel"
            prefix = f"{repeat}-{mode}"
            print(f"{prefix}: clean", flush=True)
            await invoke([*(f":{m}:clean" for m in MODULES + DEPENDENCIES),
                          *common, flag], args.output / f"{prefix}-clean.log")
            for cache in ("cold", "warm"):
                name = f"{prefix}-{cache}"
                print(name, flush=True)
                log = args.output / f"{name}.log"
                result = await invoke([*tests, *common, flag, "--init-script",
                                       str(ROOT / "tools/benchmark.init.gradle")], log)
                result.update(repeat=repeat, mode=mode, cache=cache, **evidence(log))
                if cache == "cold" and not result["kotlin_daemon_seen"]:
                    raise RuntimeError("Cold build did not observe an isolated Kotlin daemon")
                for module in MODULES:
                    shutil.copytree(ROOT / module / "build/test-results/test",
                                    args.output / name / module, dirs_exist_ok=True)
                results.append(result)
                (args.output / "results.json").write_text(
                    json.dumps(results, indent=2) + "\n", encoding="utf-8")
                print(json.dumps(result), flush=True)


if __name__ == "__main__":
    asyncio.run(main())
