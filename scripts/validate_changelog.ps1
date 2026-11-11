# Strikte Validierung von CHANGELOG.md
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$utf8 = [System.Text.Encoding]::UTF8

$changelogPath = "CHANGELOG.md"
if (-not (Test-Path $changelogPath)) {
    throw "CHANGELOG.md nicht gefunden!"
}

$rawBytes = [System.IO.File]::ReadAllBytes($changelogPath)

# 1. Kein BOM
if ($rawBytes.Length -ge 3 -and $rawBytes[0] -eq 0xEF -and $rawBytes[1] -eq 0xBB -and $rawBytes[2] -eq 0xBF) {
    throw "Fehler: CHANGELOG.md hat ein UTF-8 BOM, muss UTF-8 ohne BOM sein!"
}

# 2. Strikte UTF-8 Dekodierung
try {
    $decoder = [System.Text.UTF8Encoding]::new($false, $true)
    $text = $decoder.GetString($rawBytes)
} catch {
    throw "Fehler: CHANGELOG.md enthält ungültige UTF-8 Byte-Sequenzen!"
}

# 3. Mojibake-Prüfung
if ($text.Contains("Ã") -or $text.Contains([char]0xFFFD)) {
    throw "Fehler: Mojibake-Zeichen ('Ã' oder U+FFFD) in CHANGELOG.md gefunden!"
}

# 4. Transliterationsprüfung
$patterns = @(
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

foreach ($pat in $patterns) {
    if ($text -match $pat) {
        throw "Fehler: Verbotene Transliteration '$($matches[0])' in CHANGELOG.md gefunden! Bitte echte Umlaute verwenden."
    }
}

# 5. README.md Pruefung: Kein Geodreieck-Emoji
$readmePath = "README.md"
if (Test-Path $readmePath) {
    $firstLine = [System.IO.File]::ReadAllLines($readmePath, [System.Text.Encoding]::UTF8)[0]
    # U+1F4D0 (Geodreieck / Triangular Ruler Emoji)
    $emojiStr = [char]::ConvertFromUtf32(0x1F4D0)
    if ($firstLine.Contains($emojiStr)) {
        throw "Fehler: README.md enthaelt das Geodreieck-Emoji (U+1F4D0)!"
    }
}

Write-Host "CHANGELOG.md und README.md erfolgreich validiert (reines UTF-8, kein Mojibake, keine Transliterationen, keine unerwuenschten Emojis)." -ForegroundColor Green
