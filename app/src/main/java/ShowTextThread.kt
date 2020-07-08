import android.app.Activity
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.viplazy.speedword.ViewHolder
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ShowTextThread(
    private val activity: Activity,
    private val textContent: String,
    private val viewHolder: ViewHolder
) : Thread() {
    @Volatile
    private var running = true

    @Volatile
    private var paused = true

    @Volatile
    private var list = mutableListOf<String>()

    private val pauseLock = ReentrantLock()
    private val condition = pauseLock.newCondition()

    companion object {
        const val SPEED: Int = 300
        const val DEFAULT_DURATION: Int = 60000 / SPEED
    }

    private var duration = DEFAULT_DURATION

    var index = 0

    override fun run() {
        super.run()

        activity.runOnUiThread {
            viewHolder.textWord.text = "Initalizing..."
        }

        val stringTokenizer = StringTokenizer(textContent)

        val lineEnding = mutableListOf('.', ',', ':', '?')

        while (stringTokenizer.hasMoreTokens()) {
            list.add(stringTokenizer.nextToken())
        }

        viewHolder.progress.progress = index
        viewHolder.progress.max = list.size - 1

        activity.runOnUiThread {
            viewHolder.textWord.text = "READY"
        }

        while (running) {

            pauseLock.withLock {
                if (paused) {
                    condition.await()
                }
            }

            if (!running) { // running might have changed since we paused
                break
            }

            if (index >= list.size) {
                pause()
                continue
            }

            val word = list[index]

            activity.runOnUiThread {
                viewHolder.textWord.text = word
                viewHolder.progress.progress = index
            }

            var step = 0
            while (step < duration) {
                step += 85
                sleep(85)
            }

            if (word[word.length - 1] in lineEnding) sleep((if (duration > 300) 300 else duration).toLong())

            index += 1
        }
    }

    fun stopThread() {
        running = false
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resumeThread()
        // to unblock

        activity.runOnUiThread {
            viewHolder.textWord.text = "STOPED"
        }
    }

    fun pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true
    }

    fun resumeThread() {
        pauseLock.withLock {
            paused = false
            condition.signalAll() // Unblocks thread
        }
    }

    override fun start() {
        super.start()

        viewHolder.progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                index = progress

                val word = list[index]

                activity.runOnUiThread {
                    viewHolder.textWord.text = word
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(
                    activity,
                    String.format("%d", with(seekBar!!) { (progress + 1) * 100 / (max + 1) }) + "%",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        viewHolder.speed.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var sp = progress

                // escape division by zero
                if (sp == 0) sp += 1

                duration = 60000 / sp
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(
                    activity,
                    String.format("%d words per min", seekBar?.progress),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun isPaused() = paused
    fun isStoped() = !running
}