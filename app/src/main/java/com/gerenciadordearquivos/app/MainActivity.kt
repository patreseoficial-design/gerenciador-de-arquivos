package com.gerenciadordearquivos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var pathText: TextView
    private lateinit var backButton: Button

    private var currentDirectory: File = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.fileList)
        pathText = findViewById(R.id.pathText)
        backButton = findViewById(R.id.backButton)

        // Android 11+
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        backButton.setOnClickListener {
            val parent = currentDirectory.parentFile

            if (parent != null) {
                currentDirectory = parent
                showFiles()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->

            val files = getFiles()

            if (position < files.size) {

                val file = files[position]

                if (file.isDirectory) {
                    currentDirectory = file
                    showFiles()
                } else {
                    openFile(file)
                }
            }
        }

        showFiles()
    }

    private fun getFiles(): List<File> {
        return currentDirectory.listFiles()
            ?.sortedWith(
                compareBy<File> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            ?: emptyList()
    }

    private fun showFiles() {

        pathText.text = currentDirectory.absolutePath

        val files = getFiles()

        val names = files.map { file ->

            if (file.isDirectory) {
                "📁  ${file.name}"
            } else {
                "📄  ${file.name}"
            }

        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            names
        )

        listView.adapter = adapter
    }

    private fun openFile(file: File) {

        try {

            val uri = Uri.fromFile(file)

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "*/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }

    override fun onBackPressed() {

        val parent = currentDirectory.parentFile

        if (parent != null) {

            currentDirectory = parent
            showFiles()

        } else {

            super.onBackPressed()

        }
    }
}
