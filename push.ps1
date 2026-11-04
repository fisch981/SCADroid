param (
    [string]$ChangeSummary = "Automatisches Update",
    [string]$BumpType = "patch" # patch, minor, major
)

$ErrorActionPreference = "Stop"

# 1. Versionierung in app/build.gradle.kts auslesen & inkrementieren
$gradleFile = "app/build.gradle.kts"
$content = Get-Content $gradleFile -Raw

if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentCode = [int]$matches[1]
    $newCode = $currentCode + 1
} else {
    $newCode = 2
}

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $currentName = $matches[1]
    $parts = $currentName.Split('.')
    $major = [int]$parts[0]
    $minor = [int]$parts[1]
    $patch = [int]$parts[2]

    if ($BumpType -eq "major") {
        $major++; $minor = 0; $patch = 0
    } elseif ($BumpType -eq "minor") {
        $minor++; $patch = 0
    } else {
        $patch++
    }
    $newName = "$major.$minor.$patch"
} else {
    $newName = "0.1.1"
}

Write-Host "==> Erhöhe Version: $currentName (Code $currentCode) -> $newName (Code $newCode)" -ForegroundColor Cyan
$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newName`""
Set-Content -Path $gradleFile -Value $content -NoNewline

# 2. CHANGELOG.md aktualisieren
$changelogFile = "CHANGELOG.md"
$today = (Get-Date).ToString("yyyy-MM-dd")
$newEntry = @"
## [$newName] - $today

### Geändert / Hinzugefügt
- $ChangeSummary

"@

$existingChangelog = Get-Content $changelogFile -Raw
# Nach dem Header einfügen
if ($existingChangelog -match '(?s)(# Changelog.*?---\s*\r?\n\r?\n)(.*)') {
    $updatedChangelog = $matches[1] + $newEntry + "`n" + $matches[2]
    Set-Content -Path $changelogFile -Value $updatedChangelog -NoNewline
    Write-Host "==> Changelog für v$newName aktualisiert" -ForegroundColor Cyan
}

# 3. Release APK kompilieren
Write-Host "==> Kompiliere Release-APK v$newName..." -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
& ".\gradlew.bat" copyReleaseApkToRoot

if (-not (Test-Path "SCADroid-release.apk")) {
    Write-Error "Release APK konnte nicht im Root-Verzeichnis erstellt werden!"
}

# 4. Git Stage, Commit & Push
Write-Host "==> Stage & Commit..." -ForegroundColor Cyan
git add .
git commit -m "Release v${newName}: $ChangeSummary"

Write-Host "==> Pushe nach GitHub (origin main)..." -ForegroundColor Cyan
git push origin main

# 5. GitHub Release erstellen / aktualisieren
Write-Host "==> Erstelle GitHub Release v$newName..." -ForegroundColor Cyan
try {
    gh release create "v$newName" "SCADroid-release.apk" --title "SCADroid v$newName" --notes "$ChangeSummary"
} catch {
    Write-Host "Hinweis: GitHub Release existiert bereits oder gh fehlerhaft, versuche Upload..." -ForegroundColor Yellow
    gh release upload "v$newName" "SCADroid-release.apk" --clobber
}

Write-Host "==> Erfolgreich veröffentlicht & gepusht: v$newName (Code $newCode)" -ForegroundColor Green
