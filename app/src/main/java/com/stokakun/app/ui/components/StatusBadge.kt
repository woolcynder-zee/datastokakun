package com.stokakun.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.ui.theme.StatusAvailable
import com.stokakun.app.ui.theme.StatusReserved
import com.stokakun.app.ui.theme.StatusSold

fun statusLabel(status: AccountStatus): String = when (status) {
    AccountStatus.AVAILABLE -> "Available"
    AccountStatus.RESERVED -> "Reserved"
    AccountStatus.SOLD -> "Sold"
}

fun statusColor(status: AccountStatus) = when (status) {
    AccountStatus.AVAILABLE -> StatusAvailable
    AccountStatus.RESERVED -> StatusReserved
    AccountStatus.SOLD -> StatusSold
}

@Composable
fun StatusBadge(status: AccountStatus, modifier: Modifier = Modifier) {
    Text(
        text = statusLabel(status),
        color = statusColor(status),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(statusColor(status).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
