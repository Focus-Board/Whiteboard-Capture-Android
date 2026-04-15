package com.example.whiteboardcaptureandroid

enum class ScanMode(
    val displayName: String,
    val description: String
) {
    DOCUMENT("Auto Scan", "Default document capture"),
    WHITEBOARD("Whiteboard Beta", "Experimental whiteboard detection");

    companion object {
        fun fromOrdinal(ordinal: Int): ScanMode {
            return values()[ordinal]
        }
    }
}