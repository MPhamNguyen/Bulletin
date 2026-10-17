package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.marketplace.presentation.ListingReportViewModel

fun AppContainer.createListingReportViewModel() = ListingReportViewModel(reportMarketplaceListing)
