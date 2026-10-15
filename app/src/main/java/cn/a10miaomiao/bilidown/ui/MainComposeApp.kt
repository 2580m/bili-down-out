package cn.a10miaomiao.bilidown.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.a10miaomiao.bilidown.common.scrollable.ScaffoldNestedScrollConnection
import cn.a10miaomiao.bilidown.common.scrollable.ScaffoldScrollableState
import cn.a10miaomiao.bilidown.entity.SortMode
import cn.a10miaomiao.bilidown.ui.page.*
import cn.a10miaomiao.bilidown.ui.theme.BiliDownTheme
import cn.a10miaomiao.bilimiao.compose.animation.materialFadeThroughIn
import cn.a10miaomiao.bilimiao.compose.animation.materialFadeThroughOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComposeApp() {
    BiliDownTheme {
        val navController = rememberNavController()
        val bottomNavList = remember {
            listOf(
                BiliDownScreen.List,
                BiliDownScreen.Progress,
                BiliDownScreen.OutList,
                BiliDownScreen.More,
            )
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val enableBottomBar = currentDestination?.hierarchy?.any { i ->
            bottomNavList.indexOfFirst { j -> i.route == j.route } != -1
        }
        val scrollableState = remember { ScaffoldScrollableState() }
        val scrollConnection = remember(scrollableState) {
            ScaffoldNestedScrollConnection(scrollableState)
        }
        LaunchedEffect(currentDestination) {
            if (enableBottomBar == true) {
                scrollableState.slideUp()
            }
        }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var isHome by remember { mutableStateOf(false) }
        var sortMode by remember { mutableStateOf(SortMode.NONE) }
        var showSortDialog by remember { mutableStateOf(false) }
        val searchFocusRequester = remember { FocusRequester() }
        LaunchedEffect(currentDestination) {
            val home = currentDestination?.hierarchy?.any { it.route == BiliDownScreen.List.route } == true
            isHome = home
            if (!home) {
                isSearching = false
                searchQuery = ""
            }
        }
        LaunchedEffect(isSearching) {
            if (isSearching) {
                searchFocusRequester.requestFocus()
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val screenTitle = BiliDownScreen.getRouteName(currentDestination?.route)
                        if (isHome && isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                singleLine = true,
                                placeholder = { Text("搜索视频标题") },
                            )
                        } else {
                            Crossfade(
                                label = screenTitle,
                                targetState = screenTitle,
                            ) {
                                Text(it)
                            }
                        }
                    },
                    navigationIcon = {
                        AnimatedVisibility (enableBottomBar == false) {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(Icons.Filled.ArrowBack, null)
                            }
                        }
                    },
                    actions = {
                        if (isHome && isSearching) {
                            IconButton(
                                onClick = {
                                    isSearching = false
                                    searchQuery = ""
                                }
                            ) {
                                Icon(Icons.Filled.Close, "关闭搜索")
                            }
                        } else if (isHome) {
                            IconButton(
                                onClick = { showSortDialog = true }
                            ) {
                            Icon(AppIcons.Sort, "排序")
                            }
                            IconButton(
                                onClick = { isSearching = true }
                            ) {
                                Icon(Icons.Filled.Search, "搜索")
                            }
                            IconButton(
                                onClick = {
                                    navController.navigate(BiliDownScreen.AddApp.route)
                                }
                            ) {
                                Icon(Icons.Filled.Add, null)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollConnection)
            ) {
                MainNavHost(
                    navController = navController,
                    innerPadding = innerPadding,
                    searchQuery = searchQuery,
                    sortMode = sortMode,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    MainNavigationBar(
                        navController = navController,
                        currentDestination = currentDestination,
                        bottomNavList = bottomNavList,
                        showBottomBar = enableBottomBar == true && scrollableState.showBottomBar,
                    )
        }
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = { Text("排序方式") },
                text = {
                    Column {
                        SortMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sortMode = mode
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = sortMode == mode,
                                    onClick = {
                                        sortMode = mode
                                        showSortDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text("关闭")
                    }
                },
            )
        }
    }
        }
    }
}

private fun NavGraphBuilder.defaultComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route,
        enterTransition = { materialFadeThroughIn(initialScale = 0.85f) },
        exitTransition = { materialFadeThroughOut() },
        popEnterTransition = {  materialFadeThroughIn(initialScale = 1.15f) },
        popExitTransition = { materialFadeThroughOut() },
        arguments = arguments,
        deepLinks = deepLinks,
        content = content,
    )
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    searchQuery: String = "",
    sortMode: SortMode = SortMode.NONE,
) {
    NavHost(
        navController,
        startDestination = BiliDownScreen.List.route,
        Modifier.padding(innerPadding),
    ) {
        defaultComposable(
            BiliDownScreen.List.route,
        ) { ListPage(navController, searchQuery, sortMode) }
        defaultComposable(BiliDownScreen.More.route) { MorePage(navController) }
        defaultComposable(BiliDownScreen.Progress.route) { ProgressPage(navController) }
        defaultComposable(BiliDownScreen.OutList.route) { OutListPage(navController) }
        defaultComposable(
            BiliDownScreen.Detail.route + "?packageName={packageName}&dirPath={dirPath}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("dirPath") { type = NavType.StringType }
            )
        ) {
            val packageName = it.arguments?.getString("packageName") ?: ""
            val dirPath = it.arguments?.getString("dirPath") ?: ""
            DownloadDetailPage(navController, packageName, dirPath)
        }

        defaultComposable(
            BiliDownScreen.AddApp.route,
        ) { AddAppPage(navController) }
        defaultComposable(BiliDownScreen.About.route) { AboutPage(navController) }
    }
}

@Composable
fun MainNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    bottomNavList: List<BiliDownScreen>,
    showBottomBar: Boolean,
) {
    AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(
            initialOffsetY = { it },
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
        ),
    ){
        NavigationBar {
            bottomNavList.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val label = screen.name
                NavigationBarItem(
                    selected = isSelected,
//                                alwaysShowLabel = labelVisibility == HomePageBottomBarLabelVisibility.ALWAYS_VISIBLE,
                    icon = {
//                                    Crossfade(
//                                        label = "home-bottom-bar",
//                                        targetState = isSelected,
//                                    ) {
                        Icon(
                            screen.icon,
                            label,
                        )
//                                    }
                    },
                    label = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    },
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
