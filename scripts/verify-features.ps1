param(
    [string]$BaseUrl = 'http://localhost:8080'
)

# Build/package and start the application before running this verification script.
# The Java checks read DB_PASSWORD or .secrets/db-password.txt without printing it.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $javaExecutable)) {
        throw 'JAVA_HOME must point to the JDK used by this project.'
    }
    $jarPath = Join-Path $projectRoot 'target/medical-record-management-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw 'Run .\mvnw.cmd -B -ntp package, then start the application first.'
    }
    & .\mvnw.cmd -o -B -ntp test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }

    $libraryDirectory = Join-Path $projectRoot 'target/catalog-verification-libs'
    $resultDirectory = Join-Path $projectRoot 'target/verification'
    New-Item -ItemType Directory -Force -Path $libraryDirectory, $resultDirectory | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -like 'BOOT-INF/lib/*.jar') {
                $destination = Join-Path $libraryDirectory ([System.IO.Path]::GetFileName($entry.FullName))
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
            }
        }
    } finally { $archive.Dispose() }

    $classPath = 'target/classes;target/test-classes;target/catalog-verification-libs/*'
    $checks = @(
        @{ Class = 'mrm.features.ProjectInventoryCheck'; Arguments = @(); Log = 'inventory.txt' },
        @{ Class = 'mrm.catalog.CatalogIntegrationCheck'; Arguments = @($BaseUrl.TrimEnd('/')); Log = 'catalog.txt' },
        @{ Class = 'mrm.catalog.CompleteCatalogCheck'; Arguments = @(); Log = 'bootstrap-and-policy.txt' },
        @{ Class = 'mrm.features.FiveFeatureCheck'; Arguments = @($BaseUrl.TrimEnd('/')); Log = 'five-features.txt' },
        @{ Class = 'mrm.features.FunctionalRequirementsCheck'; Arguments = @(); Log = 'functional-requirements.txt' }
        @{ Class = 'mrm.features.ElectronicUpgradeCheck'; Arguments = @(); Log = 'electronic-upgrade.txt' }
    )
    foreach ($check in $checks) {
        $arguments = @('-cp', $classPath, $check.Class) + $check.Arguments
        & $javaExecutable @arguments | Tee-Object -FilePath (Join-Path $resultDirectory $check.Log)
        if ($LASTEXITCODE -ne 0) { throw "Verification failed: $($check.Class)" }
    }
    Write-Host 'All explicit checks passed. Logs: target/verification/'
} finally { Pop-Location }
