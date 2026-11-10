package de.fisch981.scadroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Automatisierter Test zur Sicherstellung der Changelog-Integrität:
 * - Strikte UTF-8-Kodierung ohne unvollständige Bytes
 * - Ausschluss von Mojibake (z. B. 'Ã¤', 'Ã¼', U+FFFD)
 * - Ausschluss von Transliterationen (ae, oe, ue anstelle von echten Umlauten)
 * - Durchgängige Zweisprachigkeit (DE & EN getrennt, keine identischen Fallbacks)
 */
class ChangelogTest {

    private fun findChangelogFile(): File {
        val candidates = listOf(
            File("CHANGELOG.md"),
            File("../CHANGELOG.md"),
            File("../../CHANGELOG.md")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
            ?: throw IllegalStateException("CHANGELOG.md konnte im Projektverzeichnis nicht gefunden werden!")
    }

    @Test
    fun testChangelogEncodingAndStructure() {
        val file = findChangelogFile()
        val bytes = file.readBytes()

        // 1. Strikte UTF-8 Dekodierung
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        assertNotNull("Changelog-Inhalt darf nicht null sein", text)

        // 2. Kein UTF-8 Byte-Order-Mark (BOM) im Markdown
        if (bytes.size >= 3) {
            val isBom = bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
            assertFalse("CHANGELOG.md sollte kein UTF-8 BOM enthalten", isBom)
        }

        // 3. Ausschluss von Mojibake-Zeichen
        assertFalse("Mojibake 'Ã' in CHANGELOG.md gefunden!", text.contains("Ã"))
        assertFalse("Ersetzungszeichen (U+FFFD) in CHANGELOG.md gefunden!", text.contains("\uFFFD"))
        assertFalse("Mojibake 'GeÃ¤ndert' gefunden!", text.contains("GeÃ¤ndert"))
        assertFalse("Mojibake 'HinzugefÃ¼gt' gefunden!", text.contains("HinzugefÃ¼gt"))

        // 4. Ausschluss von Umlaut-Transliterationen (ae/oe/ue statt ä/ö/ü)
        val transliterationPatterns = listOf(
            Pattern.compile("""\b[Ff]uer\b"""),
            Pattern.compile("""\b[Uu]eber\b"""),
            Pattern.compile("""\b\w*staendig\w*"""),
            Pattern.compile("""\b\w*unterstuetz\w*"""),
            Pattern.compile("""\b\w*abhaengig\w*"""),
            Pattern.compile("""\b\w*gebuendelt\w*"""),
            Pattern.compile("""\b\w*hinzugefuegt\w*"""),
            Pattern.compile("""\b\w*geaendert\w*"""),
            Pattern.compile("""\b\w*waehrend\w*"""),
            Pattern.compile("""\b\w*loesch\w*"""),
            Pattern.compile("""\b\w*menue\w*""")
        )

        for (pattern in transliterationPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group()
                assertFalse("Verbotene Transliteration '$match' in CHANGELOG.md gefunden! Bitte echte Umlaute (ä, ö, ü, ß) verwenden.", true)
            }
        }

        // 5. Überprüfung aller Release-Einträge ab v0.1.20 auf vollständige Zweisprachigkeit
        val releaseSectionRegex = Regex("""## \[([0-9]+\.[0-9]+\.[0-9]+)\]\s*-\s*[0-9]{4}-[0-9]{2}-[0-9]{2}([\s\S]*?)(?=(## \[|$))""")
        val matches = releaseSectionRegex.findAll(text).toList()
        assertTrue("Es müssen Release-Abschnitte vorhanden sein", matches.isNotEmpty())

        val deLineRegex = Regex("""-\s*\*\*DE:\*\*\s*(.+)""")
        val enLineRegex = Regex("""-\s*\*\*EN:\*\*\s*(.+)""")

        for (match in matches) {
            val version = match.groupValues[1]
            val sectionBody = match.groupValues[2]

            // Ab Version 0.1.20 ist Zweisprachigkeit verpflichtend
            val patchNum = version.substringAfterLast(".").toIntOrNull() ?: 0
            if (patchNum >= 20) {
                val deMatch = deLineRegex.find(sectionBody)
                val enMatch = enLineRegex.find(sectionBody)

                assertNotNull("Release $version benötigt einen '- **DE:**' Eintrag!", deMatch)
                assertNotNull("Release $version benötigt einen '- **EN:**' Eintrag!", enMatch)

                val deText = deMatch!!.groupValues[1].trim()
                val enText = enMatch!!.groupValues[1].trim()

                assertTrue("Release $version: DE-Text darf nicht leer sein", deText.isNotEmpty())
                assertTrue("Release $version: EN-Text darf nicht leer sein", enText.isNotEmpty())
                assertNotEquals("Release $version: EN-Text darf nicht identisch mit DE-Text sein!", deText, enText)
            }
        }
    }
}
