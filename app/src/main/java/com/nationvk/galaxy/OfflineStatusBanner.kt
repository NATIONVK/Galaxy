package com.nationvk.galaxy

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Informational network monitor; local Galaxy features do not depend on it. */
@Composable
fun OfflineStatusBanner() {
    val context = LocalContext.current
    var online by remember { mutableStateOf(hasValidatedInternet(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = hasValidatedInternet(context) }
            override fun onLost(network: Network) { online = hasValidatedInternet(context) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        try { manager.registerDefaultNetworkCallback(callback) }
        catch (_: Exception) { online = hasValidatedInternet(context) }
        onDispose { try { manager.unregisterNetworkCallback(callback) } catch (_: Exception) { } }
    }
    if (!online) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                .background(Color(0xFF352719), RoundedCornerShape(12.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = Color(0xFFFFC66D), fontSize = 10.sp)
            Text("  OFFLINE MODE  •  Local features remain available", color = Color(0xFFFFDCA8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun hasValidatedInternet(context: Context): Boolean {
    return try {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (_: Exception) { false }
}
