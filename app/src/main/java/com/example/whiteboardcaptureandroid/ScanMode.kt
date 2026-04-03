package com.example.whiteboardcaptureandroid

enum class ScanMode(
    val displayName: String,
    val description: String
) {
    WHITEBOARD("Whiteboard", "Detect whiteboards and clean them"),
    DOCUMENT("Document", "Scan documents with auto-crop");

    companion object {
        fun fromOrdinal(ordinal: Int): ScanMode {
            return values()[ordinal]
        }
    }
}