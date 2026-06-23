package com.nexgenflows.limpiador

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexgenflows.limpiador.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter

    private val uninstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { loadApps() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = AppListAdapter { appInfo -> confirmUninstall(appInfo) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        updateStorageHeader()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateUsageAccessBanner()
    }

    private fun updateUsageAccessBanner() {
        val hasAccess = AppScanner.hasUsageAccess(this)
        binding.usageAccessBanner.visibility = if (hasAccess) View.GONE else View.VISIBLE
    }

    private fun updateStorageHeader() {
        val statFs = StatFs(filesDir.path)
        val total = statFs.totalBytes
        val free = statFs.availableBytes
        val used = total - free
        binding.storageSummary.text = getString(
            R.string.storage_summary,
            Formatters.size(used),
            Formatters.size(total)
        )
    }

    private fun loadApps() {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { AppScanner.scan(applicationContext) }
            adapter.submitList(apps)
            binding.progress.visibility = View.GONE
            updateUsageAccessBanner()
            updateStorageHeader()
        }
    }

    private fun confirmUninstall(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_uninstall_title, appInfo.label))
            .setMessage(getString(R.string.confirm_uninstall_message, appInfo.label, Formatters.size(appInfo.sizeBytes)))
            .setPositiveButton(R.string.uninstall) { _, _ -> requestUninstall(appInfo) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestUninstall(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${appInfo.packageName}"))
        runCatching { uninstallLauncher.launch(intent) }
            .onFailure { Toast.makeText(this, R.string.uninstall_error, Toast.LENGTH_SHORT).show() }
    }
}
