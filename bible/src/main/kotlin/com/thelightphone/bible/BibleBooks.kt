package com.thelightphone.bible

data class Book(val name: String, val chapters: Int, val idx: Int)

data class Verse(val number: Int, val text: String)

/** Canonical 66-book list with chapter counts, in standard (non-alphabetical) Bible order. */
val BIBLE_BOOKS: List<Book> = listOf(
    "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
    "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
    "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10,
    "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalms" to 150, "Proverbs" to 31,
    "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66, "Jeremiah" to 52, "Lamentations" to 5,
    "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14, "Joel" to 3, "Amos" to 9,
    "Obadiah" to 1, "Jonah" to 4, "Micah" to 7, "Nahum" to 3, "Habakkuk" to 3,
    "Zephaniah" to 3, "Haggai" to 2, "Zechariah" to 14, "Malachi" to 4,
    "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28,
    "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6, "Ephesians" to 6,
    "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6,
    "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13, "James" to 5,
    "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1,
    "Jude" to 1, "Revelation" to 22,
).mapIndexed { idx, (name, chapters) -> Book(name, chapters, idx) }

private val BOOKS_BY_NAME: Map<String, Book> = BIBLE_BOOKS.associateBy { it.name }

/** A handful of naming variants the ESV API (or a user's search) may use for a canonical book. */
private val BOOK_NAME_ALIASES: Map<String, String> = mapOf(
    "Psalm" to "Psalms",
    "Song of Songs" to "Song of Solomon",
    "Canticles" to "Song of Solomon",
)

fun findBook(name: String): Book? {
    val trimmed = name.trim()
    return BOOKS_BY_NAME[trimmed] ?: BOOKS_BY_NAME[BOOK_NAME_ALIASES[trimmed]]
}

/** The chapter before [book]/[chapter], crossing into the previous book's last chapter at chapter 1. */
fun previousChapter(book: Book, chapter: Int): Pair<Book, Int>? {
    if (chapter > 1) return book to (chapter - 1)
    val previousBook = BIBLE_BOOKS.getOrNull(book.idx - 1) ?: return null
    return previousBook to previousBook.chapters
}

/** The chapter after [book]/[chapter], crossing into the next book's chapter 1 at the last chapter. */
fun nextChapter(book: Book, chapter: Int): Pair<Book, Int>? {
    if (chapter < book.chapters) return book to (chapter + 1)
    val nextBook = BIBLE_BOOKS.getOrNull(book.idx + 1) ?: return null
    return nextBook to 1
}
