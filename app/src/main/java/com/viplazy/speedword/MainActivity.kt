package com.viplazy.speedword

import ShowTextThread
import android.Manifest
import android.app.Activity
import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var thread: ShowTextThread? = null
    private lateinit var viewHolder: ViewHolder
    private var textContent = ""

    companion object {
        const val REQUEST_PERMISSION_STORAGE = 11
        const val PICK_FILE = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainText.setTextColor(Color.GREEN)
        mainText.text = "PICK A FILE TO START!"

        val inputStream = assets.open("file1.txt")
        textContent = String(inputStream.readBytes())

        viewHolder = ViewHolder(mainText, progressBar, speed)

        mainText.setOnClickListener {
            if (thread == null) {
                showSnackBar("Touch into BLACK AREA for Demo")
                return@setOnClickListener
            }

            thread?.let {
                if (it.isStoped()) showSnackBar("Touch into BLACK AREA to Restart")
                else {
                    if (it.isPaused()) showSnackBar("Touch into BLACK AREA to Resume")
                    else showSnackBar("Touch into BLACK AREA to Pause")
                }
            }
        }

        bgr_color.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                container.setBackgroundColor(Color.argb(255 - progress, 13, 13, 13))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    fun onTouchedScreen(view: View) {
        if (thread == null) {
            thread = ShowTextThread(this, textContent, viewHolder).apply {
                start()
            }

            return
        }
        thread?.let {
            if (!it.isStoped()) {
                if (it.isPaused()) {
                    it.resumeThread()
                } else {
                    it.pause()
                }
            } else {
                thread = ShowTextThread(this, textContent, viewHolder).apply {
                    start()
                }
            }
        }
    }

    fun showSnackBar(mess: String = "Hello from kotlin!", duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(container, mess, duration)

        snackbar.show()
    }

    //Gallery storage permission required for Marshmallow version
    private fun verifyStoragePermissions(activity: Activity?) { // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        if (permission != PackageManager.PERMISSION_GRANTED) { // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults[0] == Activity.RESULT_OK) {
                showSnackBar("Permission ALLOWED")
            } else {
                showSnackBar("Permission DENIED")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK) {

                    val selectedFile: Uri? = data?.data

                    selectedFile?.let {
                        val inputStream: InputStream = contentResolver.openInputStream(it)!!
                        textContent = String(inputStream.readBytes())

                        thread?.stopThread()
                        onTouchedScreen(View(baseContext))
                    }

                    if (selectedFile != null) {
                        showSnackBar(selectedFile.path!!)
                    }
                } else {
                    showSnackBar("Can't select!")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun pickFile() {
        verifyStoragePermissions(this@MainActivity)
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "text/*"

        val pickIntent = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.type = "text/*"

        val chooserIntent = Intent.createChooser(getIntent, "Select Text File")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            arrayOf(pickIntent)
        )

        startActivityForResult(chooserIntent, PICK_FILE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_stop -> {
                thread?.stopThread()
                showSnackBar("Stopped")
            }

            R.id.menu_pick -> {
                pickFile()
            }

            R.id.menu_paste_clipboard -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var pasteData: String

                if (clipboard.hasPrimaryClip() && (clipboard.primaryClipDescription!!.hasMimeType(
                        MIMETYPE_TEXT_PLAIN
                    ) || clipboard.primaryClipDescription!!.hasMimeType(
                        MIMETYPE_TEXT_HTML
                    ))
                ) {
                    val textItem = clipboard.primaryClip!!.getItemAt(0)

                    // Gets the clipboard as text.
                    textItem.text?.let {
                        pasteData = it.toString()

                        textContent = pasteData
                        thread?.stopThread()
                        onTouchedScreen(View(baseContext))

                        showSnackBar("Pasted from Clipboard")
                    }
                }
            }
        }

        return true
    }
}
