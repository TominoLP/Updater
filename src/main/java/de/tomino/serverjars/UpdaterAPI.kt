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
    private const val API = "https://api.github.com/repos/ZeusSeinGrossopa/UpdaterAPI/releases/latest"
    private var updaterFile: File? = null
    private val jarPath: File? = null
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

    fun downloadUpdater(destination: File) {
        updaterFile = destination
        getLatestVersion { urlCallback: String? ->
            try {
                val url = URL(urlCallback)
                FileUtils.copyURLToFile(url, destination)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getLatestVersion(consumer: Consumer<String>) {
        try {
            val connect = URL(API).openConnection() as HttpURLConnection
            connect.connectTimeout = 10000
            connect.connect()
            val `in` = connect.inputStream
            val reader = BufferedReader(InputStreamReader(`in`, StandardCharsets.UTF_8))
            if (connect.responseCode == 200) {
                val `object` = JsonParser.parseReader(reader).asJsonObject
                consumer.accept(
                    `object`.entrySet().stream().filter { (key): Map.Entry<String, JsonElement?> -> key == "assets" }
                        .findFirst().orElseThrow { RuntimeException("Can not update system") }
                        .value.asJsonArray[0].asJsonObject["browser_download_url"].asString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun update(url: String?, newFile: File, restart: Boolean) {
        if (updaterFile == null) throw NullPointerException("The downloadUpdater must be called before using this method. Alternate use the #update(updaterFile, url, newFile) method.")
        update(updaterFile, url, newFile, restart)
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
        builder.start()
    }

}
