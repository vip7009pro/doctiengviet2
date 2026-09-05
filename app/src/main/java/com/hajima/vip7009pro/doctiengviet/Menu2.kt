package com.hajima.vip7009pro.doctiengviet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hajima.vip7009pro.doctiengviet.ui.theme.DocTiengVietTheme
import kotlinx.coroutines.launch

class Menu2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocTiengVietTheme {
                Menu2Screen()
            }
        }
    }
}

private enum class DrawerDestination(
    val labelRes: Int,
    val iconRes: Int,
    val bodyRes: Int
) {
    Home(R.string.menu_home, R.drawable.ic_menu_camera, R.string.home_placeholder),
    Gallery(R.string.menu_gallery, R.drawable.ic_menu_gallery, R.string.gallery_placeholder),
    Slideshow(R.string.menu_slideshow, R.drawable.ic_menu_slideshow, R.string.slideshow_placeholder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Menu2Screen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedDestination by rememberSaveable { mutableStateOf(DrawerDestination.Home) }
    val fabMessage = stringResource(R.string.toast_fab_action)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                selectedDestination = selectedDestination,
                onSelect = { destination ->
                    selectedDestination = destination
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(selectedDestination.labelRes)) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = fabMessage
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_dialog_email),
                        contentDescription = null
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(selectedDestination.bodyRes),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    selectedDestination: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DrawerHeader()
        DrawerDestination.values().forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.labelRes)) },
                selected = destination == selectedDestination,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.xp),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.speak),
                contentDescription = stringResource(R.string.nav_header_desc),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.nav_header_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.nav_header_subtitle),
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
