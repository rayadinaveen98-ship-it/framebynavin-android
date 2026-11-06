package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.V299PersonalRecoveryPreflight
import com.framebynavin.app.personal.V299RecoveryPreflightResult
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * A deliberate, read-only action in Backup & Recovery. No snapshot is created.
 * It cannot silently write, export, restore or delete Personal or Creator data.
 */
@Composable
internal fun V299RecoveryReadinessPanel() {
    val context = LocalContext.current.applicationContext
    val checker = remember(context) { V299PersonalRecoveryPreflight(context) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<V299RecoveryPreflightResult?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("v299-recovery-readiness"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("LOCAL BACKUP READINESS",
            color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Check the records and private photos saved on this phone.",
            color = MutedText, fontSize = 12.sp)
        OutlinedButton(
            enabled = !loading,
            onClick = {
                loading = true
                scope.launch {
                    try { result = checker.inspect() }
                    finally { loading = false }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("v299-run-preflight"),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (loading) "VERIFYING LOCAL RECORDS" else "CHECK LOCAL BACKUP COVERAGE")
        }
        when (val current = result) {
            null -> Unit
            is V299RecoveryPreflightResult.Unavailable -> {
                Text(current.safeReason, color = RecRed, fontSize = 12.sp,
                    modifier = Modifier.testTag("v299-preflight-unavailable"))
            }
            is V299RecoveryPreflightResult.Checked -> {
                Text(current.counts.conciseSummary(), color = ProjectorIvory,
                    fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.testTag("v299-preflight-counts"))
                Text(current.counts.caution(),
                    color = if (current.counts.unverifiedPhotoFiles > 0) RecRed else MutedGold,
                    fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.testTag("v299-preflight-caution"))
            }
        }
        Text("This check does not make a backup or verify cloud recovery. " +
            "It only inspects local records and recorded photo files.",
            color = MutedText, fontSize = 11.sp)
    }
}
