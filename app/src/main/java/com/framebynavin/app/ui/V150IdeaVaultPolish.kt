package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaCategory
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.IdeaVaultLabels
import com.framebynavin.app.ui.theme.*

/** Advanced Idea Vault controls moved out of the main scan path without removing any filter. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun V150IdeaFilterSheet(
    visibleCategories: List<IdeaCategory>,
    statusFilter: IdeaStatus?,
    categoryFilter: IdeaCategory?,
    captureFilter: IdeaCaptureType?,
    voiceCount: Int,
    onStatus: (IdeaStatus?) -> Unit,
    onCategory: (IdeaCategory?) -> Unit,
    onCapture: (IdeaCaptureType?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        contentColor = ProjectorIvory,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 690.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("FILTER IDEAS", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("Find exactly what you need", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))

            FilterSectionTitle("STATUS")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = statusFilter == null, onClick = { onStatus(null) }, label = { Text("Any") })
                IdeaStatus.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { onStatus(status) },
                        label = { Text(IdeaVaultLabels.status(status), fontSize = 10.sp) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("CATEGORY")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = categoryFilter == null, onClick = { onCategory(null) }, label = { Text("Any") })
                visibleCategories.forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { onCategory(category) },
                        label = { Text(IdeaVaultLabels.category(category), fontSize = 10.sp) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FilterSectionTitle("CAPTURE TYPE")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = captureFilter == null, onClick = { onCapture(null) }, label = { Text("Any") })
                FilterChip(selected = captureFilter == IdeaCaptureType.VOICE, onClick = { onCapture(IdeaCaptureType.VOICE) }, label = { Text("Voice ($voiceCount)") })
                FilterChip(selected = captureFilter == IdeaCaptureType.TEXT, onClick = { onCapture(IdeaCaptureType.TEXT) }, label = { Text("Text") })
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(
                    onClick = {
                        onStatus(null)
                        onCategory(null)
                        onCapture(null)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("RESET") }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("DONE", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, color = MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
    Spacer(Modifier.height(7.dp))
}
