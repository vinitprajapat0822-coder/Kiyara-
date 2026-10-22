package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.HighThinkingAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletGlow

data class JeeTopic(
    val subject: String,
    val title: String,
    val concept: String,
    val formula: String,
    val application: String,
    val commonMistake: String,
    val practicePrompt: String
)

@Composable
fun JeeHubScreen(viewModel: MainViewModel) {
    var selectedSubject by remember { mutableStateOf("ALL") }

    val topics = listOf(
        JeeTopic(
            subject = "PHYSICS",
            title = "Lenz's Law & Induced EMF",
            concept = "Induced current creates a magnetic field that opposes the change in magnetic flux that produced it.",
            formula = "ε = - dΦ_B / dt, where Φ_B = ∫ B · dA",
            application = "Coil entering uniform magnetic field experiences magnetic braking force F = B² L² v / R.",
            commonMistake = "Forgetting the negative sign indicating direction, or misidentifying which flux component is changing.",
            practicePrompt = "KIARAA, explain a tricky JEE Advanced problem on electromagnetic induction and moving rods with Lenz's Law."
        ),
        JeeTopic(
            subject = "PHYSICS",
            title = "Rotational Mechanics & Pure Rolling",
            concept = "In pure rolling on a stationary surface, velocity of point of contact is instantaneous zero (v_cm = ωR).",
            formula = "τ_cm = I_cm α, and a_cm = α R (if no slipping)",
            application = "Rolling down an inclined plane: a = (g sin θ) / (1 + I_cm / (m R²)).",
            commonMistake = "Assuming static friction does work in pure rolling (it does NOT dissipate energy since contact point has zero displacement).",
            practicePrompt = "KIARAA, breakdown pure rolling on an inclined plane with friction direction determination."
        ),
        JeeTopic(
            subject = "CHEMISTRY",
            title = "SN1 vs SN2 Nucleophilic Substitution",
            concept = "SN1 proceeds via carbocation intermediate (racemization); SN2 proceeds via concerted backside attack (Walden inversion).",
            formula = "Rate(SN1) = k[R-X], Rate(SN2) = k[R-X][Nu⁻]",
            application = "Polar protic solvents (H2O, EtOH) favor SN1; Polar aprotic solvents (DMF, DMSO, Acetone) favor SN2.",
            commonMistake = "Ignoring steric hindrance in secondary halides or solvent effects on nucleophilicity.",
            practicePrompt = "KIARAA, test me on 3 tricky JEE Mains questions distinguishing SN1 and SN2 reaction conditions."
        ),
        JeeTopic(
            subject = "CHEMISTRY",
            title = "Coordination Chemistry & Crystal Field Theory",
            concept = "d-orbital splitting in octahedral (t2g lower, eg higher) vs tetrahedral (e lower, t2 higher) fields.",
            formula = "Δ_t = (4/9) Δ_o, CFSE = [-0.4(n_t2g) + 0.6(n_eg)] Δ_o + mP",
            application = "Predicting magnetic behavior (paramagnetic vs diamagnetic) from spectrochemical series ligands (I⁻ < Cl⁻ < H2O < NH3 < CN⁻ < CO).",
            commonMistake = "Confusing weak field (high spin) with strong field (low spin) orbital pairing.",
            practicePrompt = "KIARAA, explain Crystal Field Splitting Energy with worked examples for d4 and d7 complexes."
        ),
        JeeTopic(
            subject = "MATHEMATICS",
            title = "Integration by Parts & ILATE Strategy",
            concept = "Standard product integration rule prioritizing u via Inverse, Log, Algebraic, Trig, Exponential.",
            formula = "∫ u v dx = u ∫ v dx - ∫ [ u' (∫ v dx) ] dx",
            application = "Solving ∫ e^(ax) sin(bx) dx using cyclic recurrence and algebra.",
            commonMistake = "Forgetting negative distribution inside the integral or omitting the constant C in indefinite integrals.",
            practicePrompt = "KIARAA, explain standard JEE tricks for definite integrals using King's property: ∫_a^b f(x)dx = ∫_a^b f(a+b-x)dx."
        ),
        JeeTopic(
            subject = "MATHEMATICS",
            title = "Vector Triple Product & 3D Lines",
            concept = "VTP resolves into the plane of the two inner vectors, perpendicular to the cross product.",
            formula = "a × (b × c) = (a · c) b - (a · b) c  (\"BAC-CAB rule\")",
            application = "Finding shortest distance between two skew lines: d = |(a2 - a1) · (b1 × b2)| / |b1 × b2|.",
            commonMistake = "Assuming cross product is associative: a × (b × c) ≠ (a × b) × c.",
            practicePrompt = "KIARAA, provide a step-by-step worked problem on finding the shortest distance between skew lines in JEE 3D Geometry."
        )
    )

    val filteredTopics = if (selectedSubject == "ALL") topics else topics.filter { it.subject == selectedSubject }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(14.dp)
    ) {
        // Hub Header & JEE Sprint Tool
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated.copy(alpha = 0.85f),
            borderColor = GlassBorderCyan
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JEE 2027 MASTER HUB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = "5-Stage Mastery: Concept • Formula • Application • Traps • Practice",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // 45-min Sprint Button
                Button(
                    onClick = {
                        viewModel.triggerCapability("study_timer", "45")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2A44)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Sprint",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "45m Sprint",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subject Filter Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val subjects = listOf("ALL", "PHYSICS", "CHEMISTRY", "MATHEMATICS")
            items(subjects) { subj ->
                val isSelected = selectedSubject == subj
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceGlass)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else GlassBorderSubtle,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedSubject = subj }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = subj,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Topics List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTopics) { topic ->
                JeeTopicCard(
                    topic = topic,
                    onAskKiaraa = { prompt ->
                        viewModel.setScreen(AppScreen.TERMINAL)
                        viewModel.sendMessage(prompt)
                    },
                    onCopyFormula = { formula ->
                        viewModel.triggerCapability("clipboard_copy", formula)
                    }
                )
            }
        }
    }
}

@Composable
fun JeeTopicCard(
    topic: JeeTopic,
    onAskKiaraa: (String) -> Unit,
    onCopyFormula: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val subjectColor = when (topic.subject) {
        "PHYSICS" -> CyanGlow
        "CHEMISTRY" -> ElectricViolet
        else -> HighThinkingAccent
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = subjectColor.copy(alpha = 0.35f),
        backgroundColor = SurfaceGlass
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(subjectColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = topic.subject,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoolWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Concept
            Text(
                text = topic.concept,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolWhite.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Formula Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF080D1A))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onCopyFormula(topic.formula) }
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = topic.formula,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                    Text(
                        text = "Copy 📋",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))

                // Application
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "APPLICATION:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                    Text(
                        text = topic.application,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "COMMON JEE TRAP:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                    Text(
                        text = topic.commonMistake,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Show Less ▲" else "Deep Breakdown ▼",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = subjectColor,
                    modifier = Modifier.clickable { expanded = !expanded }
                )

                // Solve with KIARAA
                Button(
                    onClick = { onAskKiaraa(topic.practicePrompt) },
                    colors = ButtonDefaults.buttonColors(containerColor = subjectColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Ask",
                        tint = subjectColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Solve with KIARAA",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = subjectColor
                    )
                }
            }
        }
    }
}
