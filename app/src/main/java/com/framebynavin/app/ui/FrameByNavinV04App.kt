package com.framebynavin.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorViewModel

/** V0.4 reliability wrapper: re-arms valid future reminders whenever the app resumes. */
@Composable
fun FrameByNavinV04App(vm: CreatorViewModel = viewModel()) {
    val activity = LocalActivity.current as? ComponentActivity

    LaunchedEffect(Unit) {
        vm.reconcileReminders()
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    FrameByNavinV031App(vm)
}
