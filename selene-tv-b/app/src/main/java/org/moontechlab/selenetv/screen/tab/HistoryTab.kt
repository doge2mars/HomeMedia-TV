package org.moontechlab.selenetv.screen.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moontechlab.selenetv.data.Repositories
import org.moontechlab.selenetv.model.VideoInfo
import org.moontechlab.selenetv.ui.VideoCardSource
import org.moontechlab.selenetv.ui.grid.MediaGridScaffold

/**
 * 历史 Tab：全部播放记录的网格（首页「继续观看」行只留最近 6 部未看完的，
 * 这里是完整历史，含已看完的，按最近观看时间倒序）。
 * 点卡片进详情（有记录则继续观看）；删除记录走首页长按菜单或详情页。
 * 与货架 Tab 同款网格脚手架，无筛选行，首行按上键回到顶部 TabSwitcher。
 */
@Composable
fun HistoryTab(
    focusRequester: FocusRequester = remember { FocusRequester() },
    scrollState: ScrollState = rememberScrollState(),
    onVideoSelected: (VideoInfo) -> Unit = {},
    refreshKey: Int = 0,
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var items by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 无筛选行：Tab 传进来的 focusRequester 直接挂到网格内容上，
    // TabSwitcher 按下即落到首张卡片（与 HomeTab 的 Column 同一接法）。
    val contentFocusRequester = focusRequester

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val gridLayout = remember(screenWidthDp) { calculateGridLayout(screenWidthDp) }

    fun loadHistory() {
        coroutineScope.launch {
            if (items.isEmpty()) isLoading = true
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                runCatching { Repositories.userData().getPlayRecords() }
            }
            result.onSuccess { records ->
                items = records.values
                    .sortedByDescending { it.saveTime }
                    .map { it.toVideoInfo() }
            }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(refreshKey) { loadHistory() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoading && errorMessage == null && items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "还没有播放记录，看过的片子会出现在这里",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                )
            }
        } else {
            MediaGridScaffold(
                items = items,
                isLoading = isLoading,
                errorMessage = errorMessage,
                scrollState = scrollState,
                horizontalPadding = gridLayout.horizontalPadding,
                cardGap = gridLayout.cardGap,
                contentFocusRequester = contentFocusRequester,
                resetKey = refreshKey,
                itemToVideoInfo = { it },
                itemToSource = { VideoCardSource.PLAYRECORD },
                onItemClick = { item -> onVideoSelected(item) },
                onRetry = { loadHistory() },
                onLoadMore = {},
                onContentNavigateUp = { focusManager.moveFocus(FocusDirection.Up) },
                filterSection = {},
            )
        }
    }
}
