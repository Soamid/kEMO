package pl.edu.agh.kemo.simulation

import me.tongfei.progressbar.ProgressBar
import org.moeaframework.util.progress.ProgressEvent
import org.moeaframework.util.progress.ProgressListener

class ProgressDisplay(
    taskName: String,
    private val totalWork: Long,
    private val progressExtractor: (ProgressEvent) -> Long
) :
    ProgressListener {

    private val progressBar: ProgressBar by lazy {
        ProgressBar(taskName, totalWork)
    }

    override fun progressUpdate(event: ProgressEvent) {
        progressBar.stepTo(progressExtractor(event))
        if (progressBar.current >= totalWork) {
            progressBar.close()
        }
    }
}