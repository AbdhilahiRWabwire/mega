package mega.privacy.android.app.service.reporter

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter

class FirebasePerformanceReporter(
    private val firebasePerformance: FirebasePerformance
) : PerformanceReporter {

    private val traces = mutableMapOf<String, Trace>()

    override fun startTrace(traceName: String) {
        stopTrace(traceName)
        traces[traceName] = firebasePerformance.newTrace(traceName).apply { start() }
    }

    override fun putMetric(traceName: String, metricName: String, value: Long) {
        traces[traceName]?.putMetric(metricName, value)
    }

    override fun putAttribute(traceName: String, attribute: String, value: String) {
        traces[traceName]?.putAttribute(attribute, value)
    }

    override fun stopTrace(traceName: String) {
        traces[traceName]?.stop()
    }

    override fun clearTraces() {
        traces.values.forEach(Trace::stop)
        traces.clear()
    }

    override fun setEnabled(enabled: Boolean) {
        firebasePerformance.isPerformanceCollectionEnabled = enabled
    }
}
