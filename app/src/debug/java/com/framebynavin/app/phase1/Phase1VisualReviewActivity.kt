package com.framebynavin.app.phase1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * DEBUG-ONLY rendered screenshot fixture for user-approved Phase 1 Option A.
 * All data is synthetic; no cloud, auth, reminders or settings are read/written.
 * This is never included in personalStable or release variants.
 */
class Phase1VisualReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(12, 16, 22)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(12, 16, 22)
        val scene = intent.getStringExtra("phase1_screen") ?: "cloud"
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Ink)) {
                Phase1ApprovedPreview(scene)
            }
        }
    }
}

private val Ink = Phase1OptionAReviewTokens.Ink
private val Panel = Phase1OptionAReviewTokens.Panel
private val Raised = Phase1OptionAReviewTokens.Raised
private val Border = Phase1OptionAReviewTokens.Border
private val Ivory = Phase1OptionAReviewTokens.Ivory
private val Muted = Phase1OptionAReviewTokens.Muted
private val Gold = Phase1OptionAReviewTokens.Gold
private val Peach = Phase1OptionAReviewTokens.Peach
private val Coral = Phase1OptionAReviewTokens.Coral
private val Warm = Phase1OptionAReviewTokens.WarmAction
private val WarmPanel = Phase1OptionAReviewTokens.WarmPanel
private val Rounded = RoundedCornerShape(19.dp)

@Composable private fun Label(s:String){
    Text(s, color=Gold, fontSize=10.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
}
@Composable private fun Title(s:String){
    Text(s, color=Ivory, fontSize=31.sp, lineHeight=35.sp, letterSpacing=(-.8).sp, fontWeight=FontWeight.Bold)
}
@Composable private fun Note(s:String){
    Text(s,color=Muted,fontSize=13.sp,lineHeight=20.sp)
}
@Composable private fun Frame(warm:Boolean=false,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxWidth().background(if(warm) WarmPanel else Brush.linearGradient(listOf(Panel,Panel)),Rounded)
        .border(1.dp,if(warm)Color(0xFF786851) else Border,Rounded)
        .padding(17.dp), verticalArrangement=Arrangement.spacedBy(11.dp),content=content)
}
@Composable private fun Caps(s:String){
    Text(s,color=Muted,letterSpacing=1.1.sp,fontSize=10.sp,fontWeight=FontWeight.Bold)
}
@Composable private fun Primary(s:String){
    Box(Modifier.fillMaxWidth().height(50.dp).background(Warm,RoundedCornerShape(15.dp)),
        contentAlignment=Alignment.Center){
        Text(s,color=Color(0xFF241B16),fontWeight=FontWeight.Bold,fontSize=16.sp)
    }
}
@Composable private fun Secondary(s:String){
    Box(Modifier.fillMaxWidth().height(48.dp).background(Panel,RoundedCornerShape(15.dp))
        .border(1.dp,Border,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){
        Text(s,color=Ivory,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
    }
}
@Composable private fun Chip(s:String,chosen:Boolean=false) {
    Box(Modifier.border(1.dp,if(chosen) Gold else Border,RoundedCornerShape(28.dp))
        .background(if(chosen) Color(0xFF2D231E) else Color.Transparent,RoundedCornerShape(28.dp))
        .padding(horizontal=10.dp,vertical=8.dp)){
        Text(s,color=if(chosen) Ivory else Muted,fontSize=11.sp)
    }
}
@Composable private fun Header(ending:String="···") {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween){
        Text("‹",color=Ivory,fontSize=34.sp)
        Box(Modifier.size(36.dp).background(Panel,CircleShape).border(1.dp,Border,CircleShape),
            contentAlignment=Alignment.Center){
            Text(ending,color=Ivory,fontSize=19.sp)
        }
    }
}
@Composable private fun Phase1ApprovedPreview(screen:String){
    Surface(color=Ink,modifier=Modifier.fillMaxSize()){
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if(screen=="personal-hub" || screen=="creator-hub") Phase1HubVisualReview(screen)
            else if(screen=="alarm") Alarm()
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=24.dp).padding(top=20.dp,bottom=28.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)){
                Header(if(screen=="challenge-create"||screen=="delete-account")"?" else "···")
                Spacer(Modifier.height(10.dp))
                when(screen){
                    "cloud"->Cloud()
                    "challenge-create"->ChallengeCreate()
                    "challenge-dashboard"->ChallengeDashboard()
                    "delete-account"->DeleteAccount()
                    else->Text("Review screen unavailable",color=Ivory)
                }
            }
        }
    }
}
@Composable private fun Cloud(){
    Label("BACKLOT · DATA & RECOVERY")
    Title("Your cloud vault")
    Note("Back up deliberately. Restore only when you choose. Never confuse sync with recovery.")
    Spacer(Modifier.height(3.dp))
    Frame(warm=true) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Text("◌ Not backed up yet",color=Gold,fontSize=12.sp,
                modifier=Modifier.background(Color(0xFF2F231C),RoundedCornerShape(10.dp)).padding(9.dp))
            Text("☁",color=Ivory,fontSize=17.sp)
        }
        Text("No verified cloud snapshot",color=Ivory,
            fontWeight=FontWeight.Bold,fontSize=21.sp,lineHeight=25.sp)
        Note("Your data stays on this phone until you start a complete backup.")
        Column(Modifier.fillMaxWidth().background(Raised,RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)){
            Caps("COVERAGE CHECKLIST")
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Chip("Projects");Chip("Ideas");Chip("Personal")
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Chip("Spaces");Chip("Challenges");Chip("Media")
            }
            Note("Final coverage must be verified before release.")
        }
    }
    Primary("☁   Back up everything")
    Secondary("↶   Restore a backup")
    HorizontalDivider(color=Border)
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text("Delete cloud backup   ›",color=Ivory,fontSize=15.sp,fontWeight=FontWeight.Bold)
        Note("Keep your phone data. Remove cloud copies.")
    }
    Frame {
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Text("🔒",fontSize=16.sp)
            Text("Backups require authentication",color=Muted,fontSize=12.sp,
                modifier=Modifier.weight(1f))
            Text("Read more ›",color=Gold,fontSize=12.sp)
        }
    }
}
@Composable private fun ChallengeCreate() {
    Label("NEW 21-DAY CHALLENGE")
    Title("Make it stick.")
    Note("One small action. Every day. For 21 days. Choose one activity and when Backlot should remind you.")
    Spacer(Modifier.height(2.dp))
    Frame { Caps("YOUR ACTIVITY");Text("Read 20 minutes",color=Ivory,fontSize=17.sp,fontWeight=FontWeight.Bold) }
    Frame { Caps("START DATE");Text("Today, 7 Nov 2026        ▾",color=Ivory,fontSize=16.sp,fontWeight=FontWeight.Bold) }
    Frame {
        Caps("DAILY ALERT")
        Text("7:30 AM     ⌄",color=Ivory,fontSize=17.sp,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Chip("◐ Alarm",true);Chip("♫ Voice");Chip("♧ Quiet")
        }
    }
    Frame {
        Text("What happens next?",color=Ivory,fontSize=16.sp,fontWeight=FontWeight.Bold)
        Note("A daily reminder for 21 dates. Only you can mark a day complete.")
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Note("Advanced: missed days, pause, notes")
        Text("Show ›",color=Gold,fontSize=14.sp)
    }
    Primary("Start my 21 days     →")
    Text("Preview concept · Nothing scheduled yet",color=Muted,fontSize=12.sp,
        textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
}
@Composable private fun ChallengeDashboard(){
    Label("21-DAY CHALLENGE")
    Title("Read 20 minutes")
    Note("One activity, once a day. A clear path from the first day to the last.")
    Spacer(Modifier.height(2.dp))
    Frame(warm=true){
        Caps("YOU'RE ON")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Row(verticalAlignment=Alignment.Bottom){
                Text("Day 7",color=Ivory,fontSize=34.sp,fontWeight=FontWeight.Bold)
                Text(" / 21",color=Muted,fontSize=18.sp,modifier=Modifier.padding(start=5.dp,bottom=5.dp))
            }
            Text("◕",color=Gold,fontSize=34.sp)
        }
        Note("6 days completed · current streak 6")
        LinearProgressIndicator(progress={6f/21f},modifier=Modifier.fillMaxWidth()
            .height(7.dp),color=Coral,trackColor=Border)
    }
    Frame {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Your 21 days",color=Ivory,fontSize=16.sp,fontWeight=FontWeight.Bold)
            Note("6 of 21 done")
        }
        Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
            (0..2).forEach { row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    (1..7).forEach { col->
                        val day=row*7+col
                        val done=day<7
                        Box(Modifier.weight(1f).aspectRatio(1f)
                            .background(if(done) Gold else Raised,RoundedCornerShape(12.dp))
                            .border(if(day==7)2.dp else 1.dp,
                                if(day==7)Coral else if(done)Gold else Border,RoundedCornerShape(12.dp)),
                            contentAlignment=Alignment.Center){
                            Text(if(done)"✓" else "$day",
                                color=if(done)Ink else if(day==7)Ivory else Muted,
                                fontSize=12.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    Frame {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Column(Modifier.weight(1f)){
                Text("Today's activity",color=Ivory,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Note("Read 20 minutes · 7:30 AM reminder")
            }
            Text("Today",color=Gold,fontSize=12.sp)
        }
    }
    Primary("✓   Mark today done")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Note("Write a note about today");Text("+ Add note",color=Gold,fontSize=14.sp)
    }
    Text("Progress is always confirmed by you",color=Muted,
        fontSize=12.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
}
@Composable private fun DotField(){
    Canvas(Modifier.fillMaxWidth().height(210.dp)){
        val dx=size.width/16f;val dy=size.height/16f
        for(y in 1..15)for(x in 1..15){
            val rx=x-8;val ry=y-8
            val distance=sqrt((rx*rx+ry*ry).toFloat())
            val lit=abs(distance-5f)<1.4f
            drawCircle(
                color=if(lit)if((x+y)%4==0)Gold else Coral else Color(0xFF985861),
                radius=if(lit)4.4.dp.toPx() else 2.4.dp.toPx(),
                center=Offset(x*dx,y*dy),
            )
        }
    }
}
@Composable private fun Alarm(){
    Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=26.dp),
        horizontalAlignment=Alignment.CenterHorizontally){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Label("BACKLOT ALERTS")
            Text("ALARM",color=Gold,fontSize=12.sp,
                modifier=Modifier.background(Color(0xFF30231C),RoundedCornerShape(9.dp))
                    .padding(9.dp))
        }
        Spacer(Modifier.weight(.45f))
        DotField()
        Label("YOUR ALARM")
        Text("Time to read",color=Ivory,fontWeight=FontWeight.Bold,fontSize=35.sp,
            modifier=Modifier.padding(top=10.dp))
        Text("Read 20 minutes · Day 7 of 21",color=Muted,fontSize=14.sp,
            modifier=Modifier.padding(top=12.dp,bottom=18.dp))
        Frame {
            Text("Scheduled for 7:30 AM",modifier=Modifier.fillMaxWidth(),
                color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
            Text("Full-screen where Android permits",modifier=Modifier.fillMaxWidth(),
                color=Gold,textAlign=TextAlign.Center,fontSize=13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Primary("✓   Done")
        Spacer(Modifier.height(11.dp))
        Secondary("◷   Snooze 10 minutes")
        Text("A quiet notification is used if access is denied",
            color=Muted,modifier=Modifier.padding(top=13.dp),
            textAlign=TextAlign.Center,fontSize=12.sp)
        Spacer(Modifier.weight(.25f))
    }
}
@Composable private fun DeleteAccount(){
    Label("ACCOUNT & PRIVACY")
    Title("Delete your\nBacklot account")
    Note("This is different from deleting a cloud backup. Review everything that will be removed.")
    Spacer(Modifier.height(3.dp))
    Column(Modifier.fillMaxWidth().background(Color(0xFF281B21),Rounded)
        .border(1.dp,Color(0xFF713947),Rounded).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text("⚠ Permanent action",color=Color(0xFFF0B5C0),
            fontWeight=FontWeight.Bold,fontSize=17.sp)
        Text("Account removal requires recent verification. We only show ‘complete’ after the server confirms deletion.",
            color=Color(0xFFF0B5C0),fontSize=13.sp,lineHeight=20.sp)
    }
    Frame {
        Text("Data included",color=Ivory,fontSize=17.sp,fontWeight=FontWeight.Bold)
        listOf("Cloud backup history","Account and credentials",
            "Cloud media and records","Data on this phone").forEachIndexed { idx,name->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(name,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f))
                Text(if(idx==3)"Review first" else "Included ✓",color=Gold,fontSize=12.sp)
            }
            if(idx<3)HorizontalDivider(color=Border)
        }
    }
    Frame {
        Caps("STEP 1 · CONFIRM YOUR ACCOUNT")
        Text("Reauthenticate to continue    ›",color=Muted,
            fontWeight=FontWeight.Bold,fontSize=14.sp)
    }
    Box(Modifier.fillMaxWidth().height(51.dp)
        .background(Color(0xFF3A2029),RoundedCornerShape(15.dp))
        .border(1.dp,Color(0xFF99495D),RoundedCornerShape(15.dp)),
        contentAlignment=Alignment.Center){
        Text("Request account deletion",color=Color(0xFFF3BFC9),
            fontSize=15.sp,fontWeight=FontWeight.Bold)
    }
    Text("No live deletion connected in this prototype",color=Muted,
        modifier=Modifier.fillMaxWidth(),fontSize=12.sp,textAlign=TextAlign.Center)
}
