package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MyraaAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MyraaViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: MyraaViewModel = viewModel()

        // Permission launcher for RECORD_AUDIO
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
          if (isGranted) {
            viewModel.wakeEngineManager.startListening()
          }
        }

        LaunchedEffect(Unit) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          } else {
            viewModel.wakeEngineManager.startListening()
          }
        }

        MyraaAppScreen(
          viewModel = viewModel,
          onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          }
        )
      }
    }
  }
}

/**
 * Kept for test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Piyush") }
}
