package ru.prorabprime

import androidx.core.content.FileProvider

/** Serves the pictures for the share sheet; a class of its own so it does not merge with the capture provider. */
internal class ShareFileProvider : FileProvider()
