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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntry
import com.example.operating.CapabilityInfo
import com.example.operating.CapabilityStatus
import com.example.operating.RiskLevel
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CapabilitiesScreen(viewModel: MainViewModel) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val capabilities = viewModel.capabilityRegistry.capabilities
    var activeTab by remember { mutableStateOf("REGISTRY") } // "REGISTRY" vs "AUDIT"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(14.dp)
    ) {
        // Architecture Pipeline Header
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated.copy(alpha = 0.85f),
            borderColor = NeonCyan.copy(alpha = 0.4f)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Operating Layer",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERSONAL AI OPERATING LAYER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pipeline: Intent → Planner → Tool Manager → Risk Gate → Execution → Verification → Audit",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs: Capability Registry vs Audit Log
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == "REGISTRY") NeonCyan.copy(alpha = 0.2f) else SurfaceGlass)
                    .border(
                        1.dp,
                        if (activeTab == "REGISTRY") NeonCyan else GlassBorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { activeTab = "REGISTRY" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Tools",
                        tint = if (activeTab == "REGISTRY") NeonCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tool Registry (${capabilities.size})",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "REGISTRY") CoolWhite else TextMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == "AUDIT") ElectricViolet.copy(alpha = 0.25f) else SurfaceGlass)
                    .border(
                        1.dp,
                        if (activeTab == "AUDIT") ElectricViolet else GlassBorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { activeTab = "AUDIT" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Audit",
                        tint = if (activeTab == "AUDIT") ElectricViolet else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audit Logs (${auditLogs.size})",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "AUDIT") CoolWhite else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == "REGISTRY") {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(capabilities) { cap ->
                    CapabilityItemCard(
                        capability = cap,
                        onExecute = {
                            val param = when (cap.id) {
                                "study_timer" -> "25"
                                "clipboard_copy" -> "KIARAA Terminal Snippet: Verified"
                                "termux_builder" -> "python"
                                else -> "Standard parameter"
                            }
                            viewModel.triggerCapability(cap.id, param)
                        }
                    )
                }
            }
        } else {
            if (auditLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No capability actions logged yet.\nExecute any tool from the Registry to generate verified audit entries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        AuditLogRow(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilityItemCard(
    capability: CapabilityInfo,
    onExecute: () -> Unit
) {
    val riskColor = when (capability.riskLevel) {
        RiskLevel.LOW -> EmeraldSuccess
        RiskLevel.MEDIUM -> AmberWarning
        RiskLevel.HIGH -> RubyError
        RiskLevel.CRITICAL -> Color(0xFFDC2626)
    }

    val statusColor = when (capability.status) {
        CapabilityStatus.AVAILABLE -> EmeraldSuccess
        CapabilityStatus.REQUIRES_PERMISSION -> AmberWarning
        CapabilityStatus.REQUIRES_INTEGRATION -> Color(0xFFF97316)
        CapabilityStatus.PLANNED -> CyanGlow
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorderSubtle,
        backgroundColor = SurfaceGlass
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = capability.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoolWhite
                    )
                    Text(
                        text = "Platform: ${capability.platform}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Risk badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = capability.riskLevel.name,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    }

                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (capability.status) {
                                CapabilityStatus.AVAILABLE -> "AVAILABLE"
                                CapabilityStatus.REQUIRES_PERMISSION -> "PERMISSION"
                                CapabilityStatus.REQUIRES_INTEGRATION -> "OS INTEGRATION"
                                CapabilityStatus.PLANNED -> "PLANNED"
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = capability.description,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolWhite.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Verification: ${capability.verificationStrategy}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )

                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (capability.status == CapabilityStatus.AVAILABLE) NeonCyan.copy(alpha = 0.2f)
                        else SurfaceElevated
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Action",
                        tint = if (capability.status == CapabilityStatus.AVAILABLE) NeonCyan else TextDim,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (capability.status == CapabilityStatus.AVAILABLE) "Test Tool" else "Inspect",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (capability.status == CapabilityStatus.AVAILABLE) NeonCyan else TextDim
                    )
                }
            }
        }
    }
}

@Composable
fun AuditLogRow(log: AuditLogEntry) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val statusColor = when (log.status) {
        "SUCCESS" -> EmeraldSuccess
        "PENDING_CONFIRMATION" -> AmberWarning
        "BLOCKED", "FAILED" -> RubyError
        "PLANNED" -> CyanGlow
        else -> TextMuted
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassBorderSubtle,
        backgroundColor = SurfaceGlass.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = log.status,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.toolName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CoolWhite
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.details,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Text(
                text = dateStr,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDim
            )
        }
    }
}
