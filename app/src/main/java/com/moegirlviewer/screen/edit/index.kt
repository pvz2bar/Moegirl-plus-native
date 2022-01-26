package com.moegirlviewer.screen.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.imePadding
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.moegirlviewer.R
import com.moegirlviewer.component.AppHeaderIcon
import com.moegirlviewer.component.BackHandler
import com.moegirlviewer.component.commonDialog.ButtonConfig
import com.moegirlviewer.component.commonDialog.CommonAlertDialogProps
import com.moegirlviewer.component.styled.StyledTopAppBar
import com.moegirlviewer.screen.edit.tabs.preview.EditScreenPreview
import com.moegirlviewer.screen.edit.tabs.wikitextEditor.EditScreenWikitextEditor
import com.moegirlviewer.screen.edit.util.showSubmitDialogOfEdit
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.LoadStatus
import com.moegirlviewer.util.computeMd5
import com.moegirlviewer.util.imeBottomPadding
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalPagerApi
@Composable
fun EditScreen(arguments: EditRouteArguments) {
  val model: EditScreenModel = hiltViewModel()

  SideEffect {
    model.routeArguments = arguments
    model.backupId = computeMd5(
      arguments.pageName + arguments.type.name + (arguments.section ?: "")
    )
  }

  LaunchedEffect(true) {
    if (model.wikitextStatus == LoadStatus.INITIAL) {
      model.loadWikitext()
    }

    if (model.checkBackupFlag) {
      model.checkBackup()
      model.checkBackupFlag = false
    }
  }

  LaunchedEffect(model.pagerState.currentPage) {
    if (model.pagerState.currentPage == 1 && model.shouldReloadPreview) {
      model.shouldReloadPreview = false
      model.loadPreview()
    }
  }
  
  BackHandler(model.wikitextTextFieldValue.text != model.originalWikiText) {
    Globals.commonAlertDialog.show(CommonAlertDialogProps(
      content = { Text(stringResource(id = R.string.editleaveHint)) },
      secondaryButton = ButtonConfig.cancelButton(),
      onPrimaryButtonClick = {
        Globals.navController.popBackStack()
      }
    ))
  }

  model.memoryStore.Provider {
    model.cachedWebViews.Provider {
      Scaffold(
        modifier = Modifier
          .imeBottomPadding(),
        topBar = {
          ComposedHeader(
            pageName = arguments.pageName,
            editType = arguments.type
          )
        }
      ) {
        HorizontalPager(
//          count = 2,
          state = model.pagerState,
          dragEnabled = true
        ) { currentIndex ->
          if (currentIndex == 0) {
            EditScreenWikitextEditor()
          } else {
            EditScreenPreview()
          }
        }
      }
    }
  }
}

@ExperimentalPagerApi
@Composable
private fun ComposedHeader(
  pageName: String,
  editType: EditType
) {
  val model: EditScreenModel = hiltViewModel()

  LaunchedEffect(model.selectedTabIndex) {
    model.pagerState.animateScrollToPage(model.selectedTabIndex)
  }

  val titles = remember { listOf(
    Globals.context.getString(R.string.wikiText),
    Globals.context.getString(R.string.previewView)
  ) }

  val actionName = when(editType) {
    EditType.FULL -> stringResource(id = R.string.edit)
    EditType.SECTION -> stringResource(id = R.string.editSection)
    EditType.NEW_PAGE -> stringResource(id = R.string.create)
  }

  Surface(
    elevation = 5.dp
  ) {
    Column() {
      StyledTopAppBar(
        title = {
          Text(
            text = "$actionName：$pageName",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        actions = {
          AppHeaderIcon(
            image = Icons.Filled.Done,
            onClick = {
              showSubmitDialogOfEdit(model)
            }
          )
        }
      )

      TabRow(
        selectedTabIndex = model.selectedTabIndex
      ) {
        titles.forEachIndexed { index, title ->
          Tab(
            text = { Text(title) },
            selected = model.selectedTabIndex == index,
            onClick = { model.selectedTabIndex = index }
          )
        }
      }
    }
  }
}