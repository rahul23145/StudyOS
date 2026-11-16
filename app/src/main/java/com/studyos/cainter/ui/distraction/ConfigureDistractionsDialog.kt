package com.studyos.cainter.ui.distraction

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.databinding.DialogConfigureDistractionsBinding
import com.studyos.cainter.databinding.ItemDistractingAppBinding
import com.studyos.cainter.util.DistractingAppInfo
import com.studyos.cainter.util.OverlayBlockerManager
import com.studyos.cainter.util.ZeroDistractionGuard

class ConfigureDistractionsDialog : DialogFragment() {

    private var _binding: DialogConfigureDistractionsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(): ConfigureDistractionsDialog = ConfigureDistractionsDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogConfigureDistractionsBinding.inflate(layoutInflater)
        val context = requireContext()

        val currentBlocked = ZeroDistractionGuard.getBlockedPackages(context).toMutableSet()
        val appList = ZeroDistractionGuard.DEFAULT_APPS.map {
            it.copy(isBlocked = currentBlocked.contains(it.packageName))
        }.toMutableList()

        // Also add any saved custom packages that are not in DEFAULT_APPS
        val defaultPackageNames = ZeroDistractionGuard.DEFAULT_APPS.map { it.packageName }.toSet()
        for (pkg in currentBlocked) {
            if (!defaultPackageNames.contains(pkg)) {
                appList.add(DistractingAppInfo(pkg, pkg, true))
            }
        }

        lateinit var adapter: BlockedAppAdapter
        adapter = BlockedAppAdapter(appList) { pkg, isChecked ->
            if (isChecked) {
                currentBlocked.add(pkg)
            } else {
                currentBlocked.remove(pkg)
            }
            ZeroDistractionGuard.setBlockedPackages(context, currentBlocked)
        }

        binding.rvBlockedApps.layoutManager = LinearLayoutManager(context)
        binding.rvBlockedApps.adapter = adapter

        binding.btnAddCustomPackage.setOnClickListener {
            val pkg = binding.etCustomPackage.text.toString().trim()
            if (pkg.isBlank()) return@setOnClickListener
            currentBlocked.add(pkg)
            ZeroDistractionGuard.setBlockedPackages(context, currentBlocked)
            appList.add(DistractingAppInfo(pkg, pkg, true))
            adapter.notifyItemInserted(appList.size - 1)
            binding.etCustomPackage.setText("")
            Toast.makeText(context, "Added $pkg to blocked list", Toast.LENGTH_SHORT).show()
        }

        if (OverlayBlockerManager.isMiuiDevice()) {
            binding.btnMiuiPermissions.visibility = View.VISIBLE
            binding.btnMiuiPermissions.setOnClickListener {
                OverlayBlockerManager.openMiuiPermissions(context)
            }
        }

        return MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setPositiveButton("Done") { _, _ ->
                ZeroDistractionGuard.setBlockedPackages(context, currentBlocked)
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class BlockedAppAdapter(
        private val list: List<DistractingAppInfo>,
        private val onToggle: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<BlockedAppAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDistractingAppBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.tvAppName.text = item.appName
            holder.binding.tvPackageName.text = item.packageName
            holder.binding.cbBlocked.isChecked = item.isBlocked

            holder.binding.cbBlocked.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item.packageName, isChecked)
            }
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(val binding: ItemDistractingAppBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
