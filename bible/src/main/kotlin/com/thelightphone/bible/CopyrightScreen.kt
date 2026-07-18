package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

private const val ESV_COPYRIGHT_TEXT = "Unless otherwise indicated, all Scripture quotations are from the ESV® Bible " +
    "(The Holy Bible, English Standard Version®), © 2001 by Crossway, a publishing ministry of Good News " +
    "Publishers. Used by permission. All rights reserved. The ESV text may not be quoted in any publication " +
    "made available to the public by a Creative Commons license. The ESV may not be translated into any other " +
    "language.\n\n" +
    "Users may not copy or download more than 500 verses of the ESV Bible or more than one half of any book " +
    "of the ESV Bible."

class CopyrightScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Copyrights"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    LightScrollView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 1.5f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = ESV_COPYRIGHT_TEXT,
                            variant = LightTextVariant.Copy,
                            lighten = true,
                        )
                    }
                }
            }
        }
    }
}
