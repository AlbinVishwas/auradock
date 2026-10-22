package com.auradock.widget

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry and lifecycle manager for all [BaseWidget] instances.
 *
 * Widgets are registered at startup via Hilt multibinding (@IntoSet in [WidgetModule]).
 * [StandbyController] drives the lifecycle by calling [activateAll], [suspendAll],
 * and [deactivateAll] in response to state transitions.
 *
 * Widget data is broadcast on [widgetDataFlow] for consumption by [StandbyViewModel].
 */
@Singleton
class WidgetManager @Inject constructor(
    private val widgets: Set<@JvmSuppressWildcards BaseWidget>,
    private val scope: CoroutineScope
) {
    private val registry: Map<String, BaseWidget> = widgets.associateBy { it.widgetId }
    private val activeJobs = mutableMapOf<String, Job>()

    init {
        Log.d(TAG, "[WidgetManager] Widget registry loaded — ${registry.size} widget(s) registered: ${registry.keys.joinToString { it.substringAfterLast('.') }}")
    }

    private val _widgetDataFlow = MutableSharedFlow<WidgetData>(replay = 1)
    val widgetDataFlow: SharedFlow<WidgetData> = _widgetDataFlow.asSharedFlow()

    fun activateAll() {
        Log.d(TAG, "[WidgetManager] activateAll() — initializing ${registry.size} widget(s)")
        registry.values.forEach { widget ->
            Log.d(TAG, "[WidgetManager] Initializing widget: ${widget.displayName} (${widget.widgetId})")
            scope.launch { widget.onActivate() }
            val job = widget.dataFlow()
                .catch { e -> _widgetDataFlow.emit(WidgetData.Error(widget.widgetId, e.message ?: "Unknown error")) }
                .onEach { data -> _widgetDataFlow.emit(data) }
                .launchIn(scope)
            activeJobs[widget.widgetId] = job
        }
        Log.d(TAG, "[WidgetManager] All widgets initialized and data collection active")
    }

    fun suspendAll() {
        Log.d(TAG, "[WidgetManager] suspendAll() — pausing heavy widget operations")
        registry.values.forEach { widget ->
            scope.launch { widget.onSuspend() }
        }
    }

    fun deactivateAll() {
        Log.d(TAG, "[WidgetManager] deactivateAll() — cancelling ${activeJobs.size} active job(s) and releasing widget resources")
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        registry.values.forEach { widget ->
            scope.launch { widget.onDeactivate() }
        }
    }

    fun getRegisteredWidgets(): List<BaseWidget> = registry.values.toList()

    companion object {
        private const val TAG = "AuraDock"
    }
}
