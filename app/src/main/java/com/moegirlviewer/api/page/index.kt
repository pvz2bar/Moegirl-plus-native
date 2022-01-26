package com.moegirlviewer.api.page

import com.moegirlviewer.Constants
import com.moegirlviewer.api.page.bean.*
import com.moegirlviewer.request.moeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object PageApi {
  suspend fun getTruePageName(
    pageName: String? = null,
    pageId: Int? = null)
  : String? {
    val res = moeRequest(
      entity = PageInfoResBean::class.java,
      params = mutableMapOf<String, Any>().apply {
        this["action"] = "query"
        this["converttitles"] = "1"
        if (pageId == null && pageName != null) this["titles"] = pageName
        if (pageId != null) this["pageids"] = pageId
      }
    )

    return res.query.pages.values.toList().first().title
  }

  suspend fun getPageContent(
    pageName: String? = null,
    revId: Int? = null
  ) = moeRequest(
    entity = PageContentResBean::class.java,
    params = mutableMapOf<String, Any>().apply {
      this["action"] = "parse"
      this["redirects"] = "1"
      this["prop"] = "text|categories|templates|sections|images|displaytitle"
      if (pageName != null && revId == null) this["page"] = pageName
      if (revId != null) this["oldid"] = revId
    }
  )

  suspend fun getMainImage(
    pageName: String,
    size: Int = 500
  ): MainImagesResBean.Query.MapValue.Thumbnail? {
    val res = moeRequest(
      entity = MainImagesResBean::class.java,
      params = mutableMapOf<String, Any>().apply {
        this["action"] = "query"
        this["prop"] = "pageimages"
        this["titles"] = pageName
        this["pithumbsize"] = size.toString()
      }
    )

    return res.query.pages.values.toList().first().thumbnail
  }

  suspend fun getImagesUrl(imageNames: List<String>): Map<String, String> {
    return withContext(Dispatchers.IO) {
       val defers = imageNames.chunked(50).map {
        async {
          moeRequest(
            entity = ImageInfoResBean::class.java,
            params = mutableMapOf<String, Any>().apply {
              this["action"] = "query"
              this["prop"] = "imageinfo"
              this["iiprop"] = "url"
              this["titles"] = it.joinToString("|") { "${Constants.filePrefix}$it" }
            }
          )
        }
      }

      defers
        .map { it.await() }
        .flatMap { (it.query?.pages?.values) ?: emptyList() }
        .fold(mutableMapOf()) { result, item ->
          val fileName = item.title.replaceFirst(Constants.filePrefix, "")
          val fileUrl = item.imageinfo[0].url
          result[fileName] = fileUrl
          result
        }
    }
  }

  suspend fun getPageInfo(pageName: String): PageInfoResBean.Query.MapValue {
    val res = moeRequest(
      entity = PageInfoResBean::class.java,
      params = mutableMapOf<String, Any>().apply {
        this["action"] = "query"
        this["prop"] = "info"
        this["inprop"] = "protection|watched|talkid"
        this["titles"] = pageName
      }
    )

    return res.query.pages.values.toList().first()
  }
}