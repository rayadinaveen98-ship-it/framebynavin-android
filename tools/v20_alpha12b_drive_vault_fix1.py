from pathlib import Path

activity = Path('app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt')
text = activity.read_text(encoding='utf-8')
text = text.replace('import androidx.activity.compose.rememberLauncherForActivityResult\n', 'import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.compose.setContent\n')
text = text.replace('import com.google.android.gms.common.Scope\n', 'import com.google.android.gms.common.api.Scope\n')
activity.write_text(text, encoding='utf-8')

gate = Path('app/src/main/java/com/framebynavin/app/ui/V20DriveRecoveryGate.kt')
text = gate.read_text(encoding='utf-8').replace('import com.google.android.gms.common.Scope\n', 'import com.google.android.gms.common.api.Scope\n')
gate.write_text(text, encoding='utf-8')

app = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
text = app.read_text(encoding='utf-8')
text = text.replace('import com.framebynavin.app.cloud.CloudSyncActivity\n', 'import com.framebynavin.app.cloud.CloudSyncActivity\nimport com.framebynavin.app.cloud.CloudSyncManager\nimport com.framebynavin.app.cloud.DriveVaultLocalStore\n')
app.write_text(text, encoding='utf-8')
