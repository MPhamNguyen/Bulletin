package com.jdrms.bulletin.domain.home.presentation

/** The only unit conversion boundary: pointer px / px per second -> resolver dp / dp per ms. */
internal data class HomeGestureSample(
    val dxDp: Float,
    val dyDp: Float,
    val vxDpPerMs: Float = 0f,
    val vyDpPerMs: Float = 0f,
    val axis: CardGestureResolver.Axis? = null,
    val cancelled: Boolean = false
)

internal fun homeGestureSample(
    dxPx: Float,
    dyPx: Float,
    vxPxPerSecond: Float,
    vyPxPerSecond: Float,
    density: Float
): HomeGestureSample {
    require(density.isFinite() && density > 0f)
    return HomeGestureSample(
        dxPx / density,
        -dyPx / density,
        vxPxPerSecond / density / 1000f,
        -vyPxPerSecond / density / 1000f
    )
}

internal enum class HomeReleaseCommand { SAVE, PASS, CANCEL, OPEN, CLOSE }

internal fun homeReleaseCommand(release: CardGestureResolver.Release, saveAvailable: Boolean): HomeReleaseCommand =
    when (release) {
        CardGestureResolver.Release.SAVE -> if (saveAvailable) HomeReleaseCommand.SAVE else HomeReleaseCommand.CANCEL
        CardGestureResolver.Release.PASS -> HomeReleaseCommand.PASS
        CardGestureResolver.Release.CANCEL -> HomeReleaseCommand.CANCEL
        CardGestureResolver.Release.OPEN, CardGestureResolver.Release.STAY_OPEN -> HomeReleaseCommand.OPEN
        CardGestureResolver.Release.CLOSE -> HomeReleaseCommand.CLOSE
    }
