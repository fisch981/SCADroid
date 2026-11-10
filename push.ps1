param (
    [Alias("Message")]
    [string]$ChangeSummary = "Automatisches Update",
    [Alias("MessageEn")]
    [string]$ChangeSummaryEn = "",
    [string]$BumpType = "patch" # patch, minor, major
)

$ErrorActionPreference = "Stop"

# Falls keine englische Nachricht angegeben wurde, Fallback vorbereiten
$summaryDe = $ChangeSummary
$summaryEn = if ($ChangeSummaryEn -and $ChangeSummaryEn.Trim() -ne "") { $ChangeSummaryEn } else { $ChangeSummary }

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

# 2. CHANGELOG.md aktualisieren (zweisprachig: DE & EN)
$changelogFile = "CHANGELOG.md"
$today = (Get-Date).ToString("yyyy-MM-dd")
$newEntry = @"
## [$newName] - $today

### Geändert & Hinzugefügt / Changed & Added
- **DE:** $summaryDe
- **EN:** $summaryEn

"@

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$existingChangelog = [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
# Nach dem Header einfügen
if ($existingChangelog -match '(?s)(# Changelog.*?---\s*\r?\n\r?\n)(.*)') {
    $updatedChangelog = $matches[1] + $newEntry + "`n" + $matches[2]
    [System.IO.File]::WriteAllText($changelogFile, $updatedChangelog, $utf8NoBom)
    Write-Host "==> Zweisprachiger Changelog für v$newName aktualisiert (UTF-8 ohne BOM)" -ForegroundColor Cyan
}

# 3. Release APK kompilieren
Write-Host "==> Kompiliere Release-APK v$newName..." -ForegroundColor Cyan
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jdkDir = Get-ChildItem "C:\Program Files\Microsoft\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkDir) {
        $env:JAVA_HOME = $jdkDir.FullName
    }
}
Remove-Item "SCADroid-release.apk" -ErrorAction SilentlyContinue
Remove-Item "SCADroid-v*.apk" -ErrorAction SilentlyContinue

& ".\gradlew.bat" clean copyReleaseApkToRoot
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle Build ist fehlgeschlagen (Exit Code $LASTEXITCODE)!"
}

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
$releaseNotes = if ($summaryDe -ne $summaryEn) { "$summaryDe`n`n$summaryEn" } else { "$summaryDe" }
try {
    gh release create "v$newName" "SCADroid-release.apk" --title "SCADroid v$newName" --notes "$releaseNotes"
} catch {
    Write-Host "Hinweis: GitHub Release existiert bereits oder gh fehlerhaft, versuche Upload..." -ForegroundColor Yellow
    gh release upload "v$newName" "SCADroid-release.apk" --clobber
}

Write-Host "==> Erfolgreich veröffentlicht & gepusht: v$newName (Code $newCode)" -ForegroundColor Green
