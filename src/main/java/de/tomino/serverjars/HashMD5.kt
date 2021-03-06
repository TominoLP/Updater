@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package de.tomino.serverjars

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

object HashMD5 {
    @Throws(IOException::class, NoSuchAlgorithmException::class)
    operator fun get(path: Path?): String {
        val hash = StringBuilder()
        for (aByte in MessageDigest.getInstance("MD5").digest(Files.readAllBytes(path))) {
            hash.append(Character.forDigit((aByte.toInt() shr 4) and 0xF, 16))
            hash.append(Character.forDigit((aByte and 0xF).toInt(), 16))
        }
        return hash.toString()
    }
}
