package com.safecheck.android.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.safecheck.android.ui.LocalAppContainer
import com.safecheck.android.ui.home.HomeScreen
import com.safecheck.android.ui.home.HomeViewModel
import com.safecheck.android.ui.manual.AnalysisPhase
import com.safecheck.android.ui.manual.ManualCheckScreen
import com.safecheck.android.ui.manual.ManualCheckViewModel
import com.safecheck.android.ui.manual.QrScanScreen
import com.safecheck.android.ui.manual.QrScanViewModel
import com.safecheck.android.ui.document.DocumentScreen
import com.safecheck.android.ui.document.DocumentViewModel
import com.safecheck.android.ui.recovery.RecoveryScreen
import com.safecheck.android.ui.recovery.RecoveryViewModel
import com.safecheck.android.ui.auto.AutomaticProtectionScreen
import com.safecheck.android.ui.auto.AutomaticProtectionViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.circle.SafetyCircleScreen
import com.safecheck.android.ui.circle.SafetyCircleViewModel
import com.safecheck.android.ui.privacy.PrivacyScreen
import com.safecheck.android.ui.privacy.PrivacyViewModel
import com.safecheck.android.ui.result.RiskResultScreen
import com.safecheck.android.ui.result.RiskResultViewModel

/**
 * Single-Activity navigation shell with a bottom navigation bar (design.md §8).
 * Phase 1 wires the routes with placeholder destinations; later phases replace each
 * placeholder with the real screen without changing this graph's shape.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SafeCheckNavHost(
    navController: NavHostController = rememberNavController(),
    initialCaseId: String? = null,
) {
    // Deep link from a tapped risk notification -> open the corresponding Risk Result.
    LaunchedEffect(initialCaseId) {
        if (!initialCaseId.isNullOrBlank()) {
            navController.navigate(Routes.riskResult(initialCaseId))
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Base route without optional query args (e.g. "circle?caseId={caseId}" -> "circle").
    val currentBaseRoute = currentRoute?.substringBefore("?")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SafeCheck",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            val subtitle = when (currentBaseRoute) {
                                Routes.HOME -> "AI Digital Safety Copilot"
                                Routes.MANUAL_CHECK -> "Manual Inspection"
                                Routes.PROTECTION -> "Automatic Guard"
                                Routes.CIRCLE -> "Safety Circle"
                                Routes.RECOVERY -> "Emergency Recovery (1930)"
                                Routes.DOCUMENT -> "Document Analysis"
                                Routes.QR_SCAN -> "Secure QR Scan"
                                Routes.PRIVACY -> "Privacy & Controls"
                                Routes.RISK_RESULT -> "Risk Assessment"
                                else -> null
                            }
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (currentBaseRoute != null && currentBaseRoute != Routes.HOME) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                    val container = LocalAppContainer.current
                    val currentLang by container.settingsStore.selectedLanguage.collectAsState(initial = "en")

                    androidx.compose.material3.TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val next = if (currentLang == "en") "hi" else "en"
                                container.settingsStore.setSelectedLanguage(next)
                            }
                        }
                    ) {
                        Text(
                            text = if (currentLang == "hi") "हिं (HI)" else "EN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (currentBaseRoute != null && currentBaseRoute != Routes.HOME) {
                        IconButton(onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Return Home"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        bottomBar = {
            // Show the bottom bar only on top-level destinations.
            if (currentBaseRoute == null || BottomDest.entries.any { it.route == currentBaseRoute }) {
                NavigationBar {
                    BottomDest.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentBaseRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                val container = LocalAppContainer.current
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container.caseStore))
                val state by vm.state.collectAsState()
                HomeScreen(
                    state = state,
                    onManualCheck = { navController.navigate(Routes.MANUAL_CHECK) },
                    onAutomaticProtection = { navController.navigate(Routes.PROTECTION) },
                    onSafetyCircle = { navController.navigate(Routes.CIRCLE) },
                    onRecovery = { navController.navigate(Routes.RECOVERY) },
                    onPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenCase = { caseId -> navController.navigate(Routes.riskResult(caseId)) },
                    onClearHistory = vm::clearHistory,
                )
            }
            composable(Routes.PROTECTION) {
                val container = LocalAppContainer.current
                val vm: AutomaticProtectionViewModel = viewModel(
                    factory = AutomaticProtectionViewModel.Factory(
                        container.context,
                        container.settingsStore,
                        container.demoSmsTrigger,
                    )
                )
                val smsEnabled by vm.smsEnabled.collectAsState()
                val demo by vm.demoState.collectAsState()

                LaunchedEffect(demo.lastDemoCaseId) {
                    val id = demo.lastDemoCaseId
                    if (id != null) {
                        navController.navigate(Routes.riskResult(id))
                        vm.consumeDemoResult()
                    }
                }

                AutomaticProtectionScreen(
                    smsEnabled = smsEnabled,
                    demoRunning = demo.demoRunning,
                    onSetSmsEnabled = vm::setSmsEnabled,
                    onFireDemo = vm::fireDemo,
                )
            }
            composable(
                route = "${Routes.CIRCLE}?caseId={caseId}",
                arguments = listOf(navArgument("caseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                val container = LocalAppContainer.current
                val caseId = entry.arguments?.getString("caseId")
                val vm: SafetyCircleViewModel = viewModel(
                    factory = SafetyCircleViewModel.Factory(
                        container.caseStore,
                        container.contactStore,
                        container.shareToSafetyCircleUseCase,
                        caseId,
                    )
                )
                val state by vm.state.collectAsState()
                SafetyCircleScreen(
                    state = state,
                    onShare = vm::share,
                    onSimulateAdvisory = vm::simulateAdvisory,
                    onSimulateNoResponse = vm::simulateNoResponse,
                    onAddContact = vm::addContact,
                    onUpdateContact = vm::updateContact,
                    onDeleteContact = vm::deleteContact,
                    onSetPrimaryContact = vm::setPrimaryContact,
                )
            }
            composable(
                route = "${Routes.RECOVERY}?caseId={caseId}",
                arguments = listOf(navArgument("caseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                val container = LocalAppContainer.current
                val caseId = entry.arguments?.getString("caseId") ?: "adhoc"
                val vm: RecoveryViewModel = viewModel(
                    factory = RecoveryViewModel.Factory(container.recordRecoveryIncidentUseCase, caseId)
                )
                val state by vm.state.collectAsState()
                RecoveryScreen(
                    state = state,
                    onToggleAction = vm::toggleAction,
                    onNext = vm::next,
                    onBack = vm::back,
                    onFinish = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.MANUAL_CHECK) {
                val container = LocalAppContainer.current
                val vm: ManualCheckViewModel = viewModel(
                    factory = ManualCheckViewModel.Factory(
                        container.analyzeContentUseCase,
                        container.ocrExtractor,
                    )
                )
                val state by vm.state.collectAsState()

                // Navigate to the result when analysis completes, then reset the phase.
                val phase = state.phase
                LaunchedEffect(phase) {
                    if (phase is AnalysisPhase.Done) {
                        navController.navigate(Routes.riskResult(phase.caseId))
                        vm.consumeResult()
                    }
                }

                ManualCheckScreen(
                    state = state,
                    onSelect = vm::select,
                    onText = vm::onText,
                    onUrl = vm::onUrl,
                    onEmailSender = vm::onEmailSender,
                    onEmailBody = vm::onEmailBody,
                    onSubmit = vm::submit,
                    onScanQr = { navController.navigate(Routes.QR_SCAN) },
                    onPickImage = vm::analyzeImage,
                    onOpenDocument = { navController.navigate(Routes.DOCUMENT) },
                )
            }
            composable(Routes.DOCUMENT) {
                val container = LocalAppContainer.current
                val vm: DocumentViewModel = viewModel(
                    factory = DocumentViewModel.Factory(container.submitDocumentUseCase)
                )
                val state by vm.state.collectAsState()
                DocumentScreen(
                    state = state,
                    onPickPdf = { uri -> vm.analyze(uri) },
                    onUseSample = { vm.analyze(null) },
                    onOpenRecovery = { id -> navController.navigate(Routes.recoveryForCase(id)) },
                )
            }
            composable(Routes.QR_SCAN) {
                val container = LocalAppContainer.current
                val vm: QrScanViewModel = viewModel(
                    factory = QrScanViewModel.Factory(container.analyzeContentUseCase)
                )
                val phase by vm.phase.collectAsState()
                LaunchedEffect(phase) {
                    if (phase is AnalysisPhase.Done) {
                        val caseId = (phase as AnalysisPhase.Done).caseId
                        navController.navigate(Routes.riskResult(caseId)) {
                            popUpTo(Routes.QR_SCAN) { inclusive = true }
                        }
                    }
                }
                QrScanScreen(onDecoded = vm::onDecoded)
            }
            composable(Routes.PRIVACY) {
                val container = LocalAppContainer.current
                val vm: PrivacyViewModel = viewModel(
                    factory = PrivacyViewModel.Factory(container.settingsStore)
                )
                val state by vm.state.collectAsState()
                PrivacyScreen(
                    state = state,
                    onSetLargeText = vm::setLargeText,
                    onSetLanguage = vm::setLanguage,
                )
            }
            composable("${Routes.RISK_RESULT}/{caseId}") { entry ->
                val container = LocalAppContainer.current
                val caseId = entry.arguments?.getString("caseId").orEmpty()
                val vm: RiskResultViewModel = viewModel(
                    factory = RiskResultViewModel.Factory(container.caseStore, caseId)
                )
                val state by vm.state.collectAsState()
                RiskResultScreen(
                    state = state,
                    onAskSafetyCircle = { id -> navController.navigate(Routes.circleForCase(id)) },
                    onOpenRecovery = { id -> navController.navigate(Routes.recoveryForCase(id)) },
                    onCheckAnother = {
                        navController.navigate(Routes.MANUAL_CHECK) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onReturnHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name)
    }
}
