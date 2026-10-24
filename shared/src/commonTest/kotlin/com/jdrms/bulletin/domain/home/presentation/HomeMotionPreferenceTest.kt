package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeMotionPreferenceTest {
    @Test fun zero_disables_duration() { assertEquals(0f, homeMotionDurationScale(0f)) }

    @Test fun half_scale_passes_through() { assertEquals(.5f, homeMotionDurationScale(.5f)) }

    @Test fun double_scale_passes_through() { assertEquals(2f, homeMotionDurationScale(2f)) }

    @Test fun absent_setting_defaults_to_one() { assertEquals(1f, homeMotionDurationScale(null)) }

    @Test fun negative_setting_defaults_to_one() { assertEquals(1f, homeMotionDurationScale(-1f)) }

    @Test fun nonfinite_settings_default_to_one() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach {
            assertEquals(1f, homeMotionDurationScale(it))
        }
    }
}
