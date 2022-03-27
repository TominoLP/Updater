package de.tomino.serverjars

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.system.exitProcess

object Updater {
    private const val URL = "https://api.github.com/repos/TominoLP/Updater/releases/latest"
    private var DOWNLOAD_URL: String? = null
    private var needUpdate = false
    private const val CURRENT_VERSION = "1.3.4"

    fun start() {
        UpdaterAPI.setAutoDelete(true)
        UpdaterAPI.downloadUpdater(File(jarPath.parentFile.toString() + "/Updater.jar"))

        try {
            update()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun update() {
        needUpdate = checkForUpdate(true)
        if (needUpdate) {
            println("New updater Version found! Restarting...")
            UpdaterAPI.update(
                DOWNLOAD_URL,
                File(jarPath.parentFile.absoluteFile.toString() + "/" + jarPath.name),
                false
            )
            exitProcess(0)
        } else {
            println("your running on the latest version of the updater")
        }
    }

    @Throws(IOException::class)
    fun checkForUpdate(download: Boolean): Boolean {
        val connect = URL(URL).openConnection() as HttpURLConnection
        connect.connectTimeout = 10000

        connect.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connect.setRequestProperty("Content-Type", "application/json")

        connect.setRequestProperty(
            "User-Agent",
            "TominoLP/Updater (" + System.getProperty("os.name") + "; " + System.getProperty("os.arch") + ")"
        )

        connect.connect()

        val `in` = connect.inputStream
        val reader = BufferedReader(InputStreamReader(`in`, StandardCharsets.UTF_8))
        if (connect.responseCode == 200) {
            val `object` = JsonParser.parseReader(reader).asJsonObject
            var latestVersion = `object`["tag_name"].asString
            latestVersion = latestVersion.replace("v", "")
            if (latestVersion.lowercase(Locale.getDefault()).contains("pre-release")) return false
            val needUpdate = compareVersions(CURRENT_VERSION, latestVersion) == -1
            if (needUpdate && download) {
                val url =
                    `object`.entrySet().stream().filter { (key): Map.Entry<String, JsonElement?> -> key == "assets" }
                        .findFirst().orElseThrow { RuntimeException("Can not update system") }
                        .value.asJsonArray[0].asJsonObject["browser_download_url"].asString ?: return false
                DOWNLOAD_URL = url
            }
            return needUpdate
        }
        return false
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val levels1 = version1.split("\\.".toRegex()).toTypedArray()
        val levels2 = version2.split("\\.".toRegex()).toTypedArray()
        val length = levels1.size.coerceAtLeast(levels2.size)
        for (i in 0 until length) {
            val v1 = if (i < levels1.size) levels1[i].toInt() else 0
            val v2 = if (i < levels2.size) levels2[i].toInt() else 0
            val compare = v1.compareTo(v2)
            if (compare != 0) {
                return compare
            }
        }
        return 0
    }

    private val jarPath: File
        get() = File(Updater::class.java.protectionDomain.codeSource.location.path)
}
