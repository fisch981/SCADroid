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

& ".\gradlew.bat" clean testDebugUnitTest copyReleaseApkToRoot
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle Build oder Unit-Tests (inkl. ChangelogTest) sind fehlgeschlagen (Exit Code $LASTEXITCODE)!"
}

if (-not (Test-Path "SCADroid-release.apk")) {
    Write-Error "Release APK konnte nicht im Root-Verzeichnis erstellt werden!"
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
try {
    gh release create "v$newName" "SCADroid-release.apk" --title "SCADroid v$newName" --notes "$releaseNotes"
} catch {
    Write-Host "Hinweis: GitHub Release existiert bereits oder gh fehlerhaft, versuche Upload..." -ForegroundColor Yellow
    gh release upload "v$newName" "SCADroid-release.apk" --clobber
}

Write-Host "==> Erfolgreich veroeffentlicht & gepusht: v$newName (Code $newCode)" -ForegroundColor Green
