import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'

def jar(group, artifact, version=None):
    folder = cache / group / artifact
    if version:
        folder /= version
    matches = sorted(folder.rglob('*.jar'))
    matches = [p for p in matches if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    if not matches:
        raise SystemExit(f'Missing cached dependency: {group}:{artifact}. Run gradlew testDebugUnitTest online first.')
    return str(matches[-1])

compiler = jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.10')
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.10')
annotations = jar('org.jetbrains', 'annotations')
compiler_cp = os.pathsep.join([compiler, stdlib, annotations,
    jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.10'),
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.10'),
    jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm')])
junit = jar('junit', 'junit', '4.13.2')
hamcrest = jar('org.hamcrest', 'hamcrest-core')
classpath = os.pathsep.join([stdlib, annotations, junit, hamcrest])
out = root / 'build/offline-domain-tests'
out.mkdir(parents=True, exist_ok=True)
studio_java = Path('C:/Program Files/Android/Android Studio/jbr/bin/java.exe')
java = str(studio_java) if studio_java.exists() else 'java'
sources = list((root / 'app/src/main/java/com/ecoceipt/app/domain').glob('*.kt'))
sources += list((root / 'app/src/test/java/com/ecoceipt/app/domain').glob('*.kt'))
compile_command = [java, '-cp', compiler_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
    '-no-stdlib', '-no-reflect', '-jvm-target', '17', '-classpath', classpath, '-d', str(out)] + list(map(str, sources))
subprocess.run(compile_command, check=True)
result = subprocess.run([java, '-cp', os.pathsep.join([str(out), classpath]),
    'org.junit.runner.JUnitCore', 'com.ecoceipt.app.domain.DomainTest'])
sys.exit(result.returncode)
