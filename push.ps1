param (
    [Alias("Message")]
    [string]$ChangeSummary = "",

    [Alias("MessageEn")]
    [string]$ChangeSummaryEn = "",

    [string]$BumpType = "patch" # patch, minor, major
)

$ErrorActionPreference = "Stop"

# Setze UTF-8 Konsole & Prozess-Encoding
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 0. Strikte Parameter-Validierung (Verhinderung von Fallbacks & Transliterationen)
if ([string]::IsNullOrWhiteSpace($ChangeSummary)) {
    throw "Fehler: Deutsche Aenderungsbeschreibung (-ChangeSummary / -Message) ist zwingend erforderlich!"
}

if ([string]::IsNullOrWhiteSpace($ChangeSummaryEn)) {
    throw "Fehler: Englische Aenderungsbeschreibung (-ChangeSummaryEn / -MessageEn) ist zwingend erforderlich!"
}

$summaryDe = $ChangeSummary.Trim()
$summaryEn = $ChangeSummaryEn.Trim()

if ($summaryDe -ieq $summaryEn) {
    throw "Fehler: Englische Beschreibung darf nicht mit der deutschen identisch sein!"
}

# Verbotene Transliterationen (ae, oe, ue statt Umlauten) im deutschen Text pruefen
$transliterationPatterns = @(
    '\b[Ff]uer\b',
    '\b[Uu]eber\b',
    '\b\w*staendig\w*',
    '\b\w*unterstuetz\w*',
    '\b\w*abhaengig\w*',
    '\b\w*gebuendelt\w*',
    '\b\w*hinzugefuegt\w*',
    '\b\w*geaendert\w*',
    '\b\w*waehrend\w*',
    '\b\w*loesch\w*',
    '\b\w*menue\w*'
)

foreach ($pat in $transliterationPatterns) {
    if ($summaryDe -match $pat) {
        $found = $matches[0]
        throw "Validierungsfehler: Verbotene Transliteration '$found' im deutschen Text erkannt! Bitte echte deutsche Umlaute verwenden."
    }
}

# 1. Versionierung in app/build.gradle.kts auslesen & inkrementieren
$gradleFile = "app/build.gradle.kts"
$content = [System.IO.File]::ReadAllText($gradleFile, [System.Text.Encoding]::UTF8)

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

Write-Host "==> Erhoehe Version: $currentName (Code $currentCode) -> $newName (Code $newCode)" -ForegroundColor Cyan
$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newName`""
[System.IO.File]::WriteAllText($gradleFile, $content, $utf8NoBom)

# 2. CHANGELOG.md aktualisieren (zweisprachig: DE & EN mit codepage-sicherem Header)
$changelogFile = "CHANGELOG.md"
$today = (Get-Date).ToString("yyyy-MM-dd")

# Kodierungsresistente Header-Zeile mit .NET Unicode-Charactern
$ae = [char]0x00E4
$ue = [char]0x00FC
$headerLine = "### Ge" + $ae + "ndert & Hinzugef" + $ue + "gt / Changed & Added"

$newEntry = "## [$newName] - $today`n`n$headerLine`n- **DE:** $summaryDe`n- **EN:** $summaryEn`n`n"

$existingChangelog = [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
if ($existingChangelog -match '(?s)(# Changelog.*?---\s*\r?\n\r?\n)(.*)') {
    $updatedChangelog = $matches[1] + $newEntry + $matches[2]
    [System.IO.File]::WriteAllText($changelogFile, $updatedChangelog, $utf8NoBom)
    Write-Host "==> Zweisprachiger Changelog fuer v$newName aktualisiert (UTF-8 ohne BOM)" -ForegroundColor Cyan
} else {
    throw "Fehler: Header im CHANGELOG.md konnte nicht gematcht werden!"
}

# 3. Vorabpruefung: CHANGELOG.md auf Mojibake und Integritaet pruefen
$clCheck = [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
if ($clCheck.Contains([char]0x00C3) -or $clCheck.Contains([char]0xFFFD)) {
    throw "Kritischer Fehler: Mojibake in CHANGELOG.md nach dem Schreiben erkannt! Abbruch."
}

# 3b. OpenSCAD Desktop Parity Test Suite ausfuehren
Write-Host "==> Fuehre OpenSCAD Desktop-Paritaetstests aus..." -ForegroundColor Cyan
node "scripts/verify_openscad_parity.js"
if ($LASTEXITCODE -ne 0) {
    Write-Error "OpenSCAD Desktop-Paritaetstests (verify_openscad_parity.js) sind fehlgeschlagen (Exit Code $LASTEXITCODE)!"
}

# 4. Release APK kompilieren inklusive automatischer Unit-Tests (ChangelogTest)
Write-Host "==> Kompiliere & teste Release-APK v$newName..." -ForegroundColor Cyan
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jdkDir = Get-ChildItem "C:\Program Files\Microsoft\jdk-17*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkDir) {
        $env:JAVA_HOME = $jdkDir.FullName
    }
}
Remove-Item "SCADroid-release.apk" -ErrorAction SilentlyContinue
Remove-Item "SCADroid-v*.apk" -ErrorAction SilentlyContinue

& ".\gradlew.bat" clean testDebugUnitTest assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle Build oder Unit-Tests (inkl. ChangelogTest) sind fehlgeschlagen (Exit Code $LASTEXITCODE)!"
}

$apkFile = Join-Path $PSScriptRoot "app\build\outputs\apk\release\SCADroid-release.apk"
if (-not (Test-Path $apkFile)) {
    $fallbackApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $fallbackApk) {
        $apkFile = $fallbackApk
    } else {
        Write-Error "Release APK konnte nicht unter $apkFile erstellt werden!"
    }
}

# 5. Git Stage, Commit & Push
Write-Host "==> Stage & Commit..." -ForegroundColor Cyan
git add .
git commit -m "Release v${newName}: $summaryDe"

Write-Host "==> Pushe nach GitHub (origin main)..." -ForegroundColor Cyan
git push origin main

# 6. GitHub Release erstellen / aktualisieren
Write-Host "==> Erstelle GitHub Release v$newName..." -ForegroundColor Cyan
$releaseNotes = "$summaryDe`n`n$summaryEn"
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
gh release view "v$newName" 2>$null | Out-Null
$releaseExists = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = $prevEap
if ($releaseExists) {
    gh release upload "v$newName" "$apkFile#SCADroid-release.apk" --clobber
} else {
    gh release create "v$newName" "$apkFile#SCADroid-release.apk" --title "SCADroid v$newName" --notes "$releaseNotes"
}
if ($LASTEXITCODE -ne 0) {
    throw "GitHub Release Upload für v$newName fehlgeschlagen!"
}

Write-Host "==> Erfolgreich veroeffentlicht & gepusht: v$newName (Code $newCode)" -ForegroundColor Green
