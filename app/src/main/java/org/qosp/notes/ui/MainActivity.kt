package org.qosp.notes.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.qosp.notes.R
import org.qosp.notes.databinding.ActivityMainBinding
import org.qosp.notes.ui.utils.closeAndThen
import org.qosp.notes.ui.utils.hideKeyboard
import org.qosp.notes.ui.utils.navigateSafely
import org.qosp.notes.ui.widget.WidgetUpdateHelper

class MainActivity : BaseActivity() {

    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding
    private val activityModel: ActivityViewModel by viewModel()

    private val topLevelMenu get() = binding.navigationView.menu

    private val primaryDestinations = setOf(
        R.id.fragment_main,
        R.id.fragment_archive,
        R.id.fragment_deleted,
    )
    private val secondaryDestinations = setOf(
        R.id.fragment_about,
        R.id.fragment_editor,
        R.id.fragment_search,
        R.id.fragment_settings,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()

        // androidx.fragment:1.3.3 caused the FragmentContainerView to apply padding to itself when
        // the attribute fitsSystemWindows is enabled. We override it here and let the fragments decide their padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { _, insets ->
            insets
        }

        // Apply insets to the NavigationView to prevent it from overlapping with the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }

        setupDrawerHeader()

        WidgetUpdateHelper.updateAllWidgets(this)
        if (intent != null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START)
        } else if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            moveTaskToBack(true)
        }
    }


    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "org.qosp.notes.NEW_NOTE" -> {
                // Request to create a new note from the widget
                val args = bundleOf("transitionName" to "", "newNoteTitle" to "")
                navController.handleDeepLink(getDeepLink(args))
                return
            }

            else -> {
                // Request to open a note from the widget
                val noteId = intent.getLongExtra("noteId", -1L)
                if (noteId > 0) {
                    val args = bundleOf("noteId" to noteId, "transitionName" to "")
                    navController.handleDeepLink(getDeepLink(args))
                    return
                } else {
                    navController.handleDeepLink(intent)
                }
            }
        }
    }

    private fun getDeepLink(args: Bundle): Intent? = NavDeepLinkBuilder(this@MainActivity)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.fragment_editor)
        .setArguments(args)
        .createTaskStackBuilder()
        .first()

    private fun setupDrawerHeader() {
        val header = binding.navigationView.getHeaderView(0)
        val syncSettingsButton =
            header.findViewById<AppCompatImageButton>(R.id.button_sync_settings)
        // Hidden/Removed sync UI logic
        syncSettingsButton.isVisible = false

        // Fixes bug that causes the header to have large padding when the keyboard is open
        ViewCompat.setOnApplyWindowInsetsListener(header) { _, insets ->
            header.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun selectCurrentDestinationMenuItem(
        destinationId: Int? = null,
    ) {
        val destinationId =
            when (val id = destinationId ?: navController.currentDestination?.id ?: return) {
                // Assign destinations that do not have a drawer entry to an existing entry
                R.id.fragment_settings -> R.id.fragment_settings
                R.id.fragment_search -> R.id.fragment_main
                else -> id
            }

        binding.navigationView.post {
            topLevelMenu.children.forEach { item ->
                item.isChecked = item.itemId == destinationId
            }
        }
    }

    private fun setupDrawerMenuItems() {
        topLevelMenu.children.forEach { item ->
                if (item.itemId !in primaryDestinations + secondaryDestinations) return@forEach

                item.setOnMenuItemClickListener {
                    binding.drawer.closeAndThen {
                        navController.navigateSafely(item.itemId)
                    }
                    false
                }
            }
    }

    private fun setupNavigation() {
        appBarConfiguration = AppBarConfiguration(
            primaryDestinations,
            binding.drawer
        )

        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        setupDrawerMenuItems()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFocus?.hideKeyboard()
            selectCurrentDestinationMenuItem(destination.id)

            setDrawerEnabled(destination.id != R.id.fragment_editor)
        }
    }

    private fun setDrawerEnabled(enabled: Boolean) {
        binding.drawer.setDrawerLockMode(
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    fun restoreNotes(uri: android.net.Uri) {
        // Feature removed/disabled in checklist mode
    }

    fun startBackup(uri: android.net.Uri) {
        // Feature removed/disabled in checklist mode
    }
}
