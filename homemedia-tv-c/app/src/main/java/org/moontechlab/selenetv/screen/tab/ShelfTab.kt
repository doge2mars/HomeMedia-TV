package org.moontechlab.selenetv.screen.tab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moontechlab.selenetv.model.CatalogItem
import org.moontechlab.selenetv.model.VideoInfo
import org.moontechlab.selenetv.service.ApiService
import org.moontechlab.selenetv.ui.FilterOption
import org.moontechlab.selenetv.ui.FilterSelector
import org.moontechlab.selenetv.ui.VideoCardSource
import org.moontechlab.selenetv.ui.grid.GridFilterRow
import org.moontechlab.selenetv.ui.grid.MediaGridScaffold

/** 货架分类：key 直接作 /api/catalog 的 type 参数（kind 值），all = 不传。 */
private val SHELF_KIND_OPTIONS = listOf(
    FilterOption("all", "全部"),
    FilterOption("电影", "电影"),
    FilterOption("剧集", "剧集"),
    FilterOption("动漫", "动漫"),
    FilterOption("短剧", "短剧"),
    FilterOption("综艺", "综艺"),
    FilterOption("其他", "其他")
)

/**
 * 货架 Tab：服务端自有目录库浏览（区别于电影/剧集等豆瓣榜单 Tab）。
 * 分类 + 地区筛选，卡片点开按标题搜索进详情，播放走专用版默认的服务端代理。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfTab(
    categoryFocusRequester: FocusRequester = remember { FocusRequester() },
    scrollState: ScrollState = rememberScrollState(),
    onVideoSelected: (VideoInfo) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var kind by remember { mutableStateOf("all") }
    var region by remember { mutableStateOf("all") }
    var regionOptions by remember { mutableStateOf(listOf(FilterOption("all", "全部"))) }
    var items by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val regionFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val gridLayout = remember(screenWidthDp) { calculateGridLayout(screenWidthDp) }

    fun loadCatalog() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    ApiService.getCatalog(
                        kind = if (kind == "all") null else kind,
                        region = if (region == "all") null else region
                    )
                }
            }
            result.onSuccess { items = it }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun loadRegions() {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    ApiService.getCatalogRegions(if (kind == "all") null else kind)
                }
            }
            result.onSuccess { list ->
                regionOptions = listOf(FilterOption("all", "全部")) +
                    list.map { FilterOption(it, it) }
            }
        }
    }

    // 分类变化只换地区下拉；地区复位放在分类点选处，与 kind 同批提交，目录只拉一次。
    LaunchedEffect(kind) { loadRegions() }
    LaunchedEffect(kind, region) { loadCatalog() }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaGridScaffold(
            items = items,
            isLoading = isLoading,
            errorMessage = errorMessage,
            scrollState = scrollState,
            horizontalPadding = gridLayout.horizontalPadding,
            cardGap = gridLayout.cardGap,
            contentFocusRequester = contentFocusRequester,
            resetKey = "$kind|$region",
            itemToVideoInfo = { it.toVideoInfo() },
            itemToSource = { VideoCardSource.SEARCH },
            onItemClick = { item -> onVideoSelected(item.toVideoInfo()) },
            onRetry = { loadCatalog() },
            onLoadMore = {},
            onContentNavigateUp = { regionFocusRequester.requestFocus() },
            filterSection = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GridFilterRow(
                        label = "分类",
                        content = {
                            FilterSelector(
                                options = SHELF_KIND_OPTIONS,
                                selectedKey = kind,
                                onSelect = { key ->
                                    if (key != kind) {
                                        kind = key
                                        region = "all"
                                    }
                                },
                                focusRequester = categoryFocusRequester,
                                modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        keyEvent.key == Key.DirectionDown
                                    ) {
                                        regionFocusRequester.requestFocus()
                                        true
                                    } else false
                                }
                            )
                        }
                    )
                    GridFilterRow(
                        label = "地区",
                        content = {
                            FilterSelector(
                                options = regionOptions,
                                selectedKey = region,
                                onSelect = { region = it },
                                focusRequester = regionFocusRequester
                            )
                        }
                    )
                }
            }
        )
    }
}
