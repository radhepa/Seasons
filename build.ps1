# Builds the mod and runs the unit tests with JDK 25 (from the user-level JAVA_HOME).
#   .\build.ps1                     build + unit tests -> build\libs\turning-seasons-<version>.jar
#   .\build.ps1 runClientGameTest   any other Gradle task(s) instead of "build"
$ErrorActionPreference = 'Stop'
$jdk = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
if (-not $jdk -or -not (Test-Path (Join-Path $jdk 'bin\javac.exe'))) {
    throw 'Install a 64-bit JDK 25 and set JAVA_HOME before building.'
}
$env:JAVA_HOME = $jdk
$env:Path = (Join-Path $jdk 'bin') + ';' + $env:Path
$tasks = if ($args.Count -gt 0) { $args } else { @('build') }
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat @tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
} finally { Pop-Location }
