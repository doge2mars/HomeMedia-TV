package org.moontechlab.selenetv.screen.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.moontechlab.selenetv.model.LiveChannel
import org.moontechlab.selenetv.model.LiveChannelGroup

private val LiveAccent = Color(0xFF4CAF50)
private val DrawerWidth = 310.dp
private const val AllTab = "全部"

internal fun livePanelChannelsForTab(groups: List<LiveChannelGroup>, tab: String): List<LiveChannel> =
    if (tab == AllTab) groups.flatMap { it.channels } else groups.find { it.name == tab }?.channels.orEmpty()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveChannelPanel(
    groups: List<LiveChannelGroup>,
    currentChannelId: String,
    userAgent: String,
    onPickChannel: (LiveChannel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tabs = remember(groups) { listOf(AllTab) + groups.map { it.name } }
    val initialTab = remember(groups, currentChannelId) {
        groups.find { g -> g.channels.any { it.id == currentChannelId } }?.name ?: AllTab
    }
    var selectedTab by remember(tabs) { mutableStateOf(initialTab) }
    var focusInGroup by remember { mutableStateOf(false) }

    val groupFocusMap = remember(tabs) { tabs.associateWith { FocusRequester() } }
    val channels = remember(groups, selectedTab) { livePanelChannelsForTab(groups, selectedTab) }
    val channelFocusMap = remember(channels) { channels.associate { it.id to FocusRequester() } }
    val activeChannelIdx = remember(channels, currentChannelId) {
        channels.indexOfFirst { it.id == currentChannelId }.coerceAtLeast(0)
    }
    val initialTabIdx = remember(tabs, initialTab) { tabs.indexOf(initialTab).coerceAtLeast(0) }
    val groupListState = rememberLazyListState(initialFirstVisibleItemIndex = initialTabIdx)
    val channelListState = rememberLazyListState(initialFirstVisibleItemIndex = activeChannelIdx)

    fun focusCurrentGroup() {
        val idx = tabs.indexOf(selectedTab).coerceAtLeast(0)
        scope.launch {
            runCatching { groupListState.scrollToItem(idx) }
            kotlinx.coroutines.delay(16)
            val fr = groupFocusMap[selectedTab] ?: groupFocusMap[tabs.firstOrNull()]
            if (fr != null && runCatching { fr.requestFocus() }.isSuccess) {
                focusInGroup = true
            }
        }
    }

    fun focusCurrentChannel() {
        val targetCh = channels.getOrNull(activeChannelIdx) ?: channels.firstOrNull()
        val fr = targetCh?.let { channelFocusMap[it.id] }
        if (fr != null && runCatching { fr.requestFocus() }.isSuccess) {
            focusInGroup = false
        }
    }

    LaunchedEffect(Unit) {
        repeat(8) {
            kotlinx.coroutines.delay(30)
            val targetCh = channels.getOrNull(activeChannelIdx) ?: channels.firstOrNull()
            val target = targetCh?.let { channelFocusMap[it.id] } ?: groupFocusMap[selectedTab]
            if (target != null && runCatching { target.requestFocus() }.isSuccess) {
                focusInGroup = targetCh == null
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (channels.isNotEmpty()) {
            val idx = channels.indexOfFirst { it.id == currentChannelId }.coerceAtLeast(0)
            channelListState.scrollToItem(idx)
        }
    }

    Row(
        modifier = modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .focusGroup()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> {
                        if (!focusInGroup) {
                            focusCurrentGroup()
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        if (focusInGroup) {
                            focusCurrentChannel()
                        } else {
                            onClose()
                        }
                        true
                    }
                    else -> false
                }
            }
            .background(
                Brush.horizontalGradient(
                    0f to Color(0xA80A0A0F),
                    0.78f to Color(0x880A0A0F),
                    1f to Color(0x550A0A0F),
                ),
            )
            .padding(start = 14.dp, end = 12.dp, top = 22.dp, bottom = 22.dp),
    ) {
        // 左栏：分组列表
        Column(modifier = Modifier.width(96.dp).fillMaxHeight()) {
            Text(
                text = "分组",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                state = groupListState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(tabs, key = { _, t -> t }) { index, tab ->
                    val isSelected = tab == selectedTab
                    var focused by remember { mutableStateOf(false) }
                    val fr = groupFocusMap[tab] ?: remember { FocusRequester() }
                    Surface(
                        onClick = {
                            selectedTab = tab
                            focusCurrentChannel()
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(BorderStroke(2.dp, LiveAccent)),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fr)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) {
                                    focusInGroup = true
                                    selectedTab = tab
                                }
                            }
                            .focusProperties {
                                if (index == 0) up = FocusRequester.Cancel
                                if (index == tabs.lastIndex) down = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                            },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || focused) FontWeight.W700 else FontWeight.W500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.14f)))
        Spacer(modifier = Modifier.width(8.dp))

        // 右栏：当前分组下的频道列表
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = "$selectedTab (${channels.size})",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                state = channelListState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(channels, key = { idx, ch -> "${ch.id}_$idx" }) { index, ch ->
                    val isCurrent = ch.id == currentChannelId
                    var focused by remember { mutableStateOf(false) }
                    val chFr = channelFocusMap[ch.id] ?: remember { FocusRequester() }
                    Surface(
                        onClick = { onPickChannel(ch) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) LiveAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(BorderStroke(2.dp, LiveAccent)),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(chFr)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) focusInGroup = false
                            }
                            .focusProperties {
                                if (index == 0) up = FocusRequester.Cancel
                                if (index == channels.lastIndex) down = FocusRequester.Cancel
                            },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = String.format("%02d", index + 1),
                                color = if (focused) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.W600,
                            )
                            Text(
                                text = ch.name,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent || focused) FontWeight.W700 else FontWeight.W500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (focused) Modifier.basicMarquee() else Modifier),
                            )
                            if (isCurrent) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LiveAccent))
                            }
                        }
                    }
                }
            }
        }
    }
}
