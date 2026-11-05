package org.moontechlab.selenetv.model

/**
 * HomeMedia 服务端货架（/api/catalog）条目。
 * 只有标题、海报、年份等元数据，没有 source/vod_id，点开走标题搜索进现有详情流。
 */
data class CatalogItem(
    val title: String,
    val poster: String,
    val year: String,
    val region: String,
    val genre: String,
    val type: String,
    val doubanId: String,
    val rating: String,
    val synopsis: String
) {
    /** 转给详情入口：source/id 为空，详情页按 searchTitle + year 做标题匹配。 */
    fun toVideoInfo(): VideoInfo = VideoInfo(
        id = "",
        source = "",
        title = title,
        sourceName = "",
        year = year,
        cover = poster,
        index = 0,
        totalEpisodes = 0,
        playTime = 0L,
        totalTime = 0L,
        saveTime = 0L,
        searchTitle = title
    )
}
