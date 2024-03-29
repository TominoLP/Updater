package de.tomino.serverjars

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

object UpdaterAPI {
    const val GITHUB_CUSTOM_URL = "https://api.github.com/repos/%s/releases/latest"
    var currentUpdater: File? = null
        private set
    private var autoDelete = false
    val jarPath: File? = null
        get() {
            if (field == null) {
                try {
                    return File(UpdaterAPI::class.java.protectionDomain.codeSource.location.toURI().path).absoluteFile
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
            return field
        }

    @JvmOverloads
    fun downloadUpdater(destination: File?, consumer: Consumer<File?>? = null) {
        var destination = destination
        destination =
            File(if (destination!!.isDirectory) File(destination.absolutePath).toString() + "/Updater.jar" else destination.absolutePath)
        val finalDestination: File = destination
        currentUpdater = finalDestination
        if (autoDelete) {
            if (destination.exists()) destination.delete()
            consumer?.accept(destination)
            return
        }
        getLatestReleaseFromGithub("ZeusSeinGrossopa", "UpdaterAPI") { callback: Array<String?> ->
            try {
                val url = URL(callback[1])
                FileUtils.copyURLToFile(url, finalDestination)
                consumer?.accept(finalDestination)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getLatestReleaseFromGithub(githubUser: String, repository: String, consumer: Consumer<Array<String?>>) {
        try {
            val connect =
                URL(String.format(GITHUB_CUSTOM_URL, "$githubUser/$repository")).openConnection() as HttpURLConnection
            connect.connectTimeout = 10000
            connect.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connect.setRequestProperty("Content-Type", "application/json")
            connect.setRequestProperty(
                "User-Agent",
                githubUser + "/" + repository + " (" + System.getProperty("os.name") + "; " + System.getProperty("os.arch") + ")"
            )
            connect.connect()
            val `in` = connect.inputStream
            val reader = BufferedReader(InputStreamReader(`in`, StandardCharsets.UTF_8))
            if (connect.responseCode == 200) {
                val `object` = JsonParser.parseReader(reader).asJsonObject
                val downloadLink =
                    `object`.entrySet().stream().filter { (key): Map.Entry<String, JsonElement?> -> key == "assets" }
                        .findFirst().orElseThrow { RuntimeException("Can not update system") }
                        .value.asJsonArray[0].asJsonObject["browser_download_url"].asString
                consumer.accept(arrayOf(`object`["tag_name"].asString, downloadLink))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun update(url: String?, newFile: File) {
        if (currentUpdater == null) throw NullPointerException("The downloadUpdater must be called before using this method. Alternate use the #update(updaterFile, url, newFile) method.")
        update(currentUpdater, url, newFile)
    }

    @Throws(IOException::class)
    fun update(url: String?, newFile: File, restart: Boolean) {
        if (currentUpdater == null) throw NullPointerException("The downloadUpdater must be called before using this method. Alternate use the #update(updaterFile, url, newFile) method.")
        update(currentUpdater, url, newFile, restart)
    }

    @Throws(IOException::class)
    fun update(updaterFile: File?, url: String?, newFile: File) {
        update(updaterFile, jarPath, url, newFile, false)
    }

    @Throws(IOException::class)
    fun update(updaterFile: File?, url: String?, newFile: File, restart: Boolean) {
        update(updaterFile, jarPath, url, newFile, restart)
    }

    @Throws(IOException::class)
    fun update(updaterFile: File?, oldFile: File?, url: String?, newFile: File, restart: Boolean) {
        val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val builder = ProcessBuilder(
            javaBin,
            "-jar",
            updaterFile!!.absolutePath,
            url,
            oldFile!!.absolutePath,
            newFile.absolutePath,
            if (restart) "true" else ""
        )
        if (autoDelete) {
            autoDelete = false
            downloadUpdater(updaterFile) { file: File? ->
                try {
                    builder.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            autoDelete = true
        } else {
            builder.start()
        }
    }

    fun needUpdate(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) == -1
    }

    fun compareVersions(version1: String, version2: String): Int {
        val levels1 = version1.split("\\.").toTypedArray()
        val levels2 = version2.split("\\.").toTypedArray()
        val length = Math.max(levels1.size, levels2.size)
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

    fun setAutoDelete(value: Boolean) {
        autoDelete = value
    }
}
