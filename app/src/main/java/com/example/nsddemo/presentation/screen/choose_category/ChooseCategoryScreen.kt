package com.example.nsddemo.presentation.screen.choose_category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.domain.util.Categories
import com.example.nsddemo.presentation.screen.choose_category.components.CategoryCard
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ChooseCategoryScreen(viewModel: ChooseCategoryViewModel, navController: NavHostController) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.choose_a_category),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(32.dp))
        LazyHorizontalGrid(
            modifier = Modifier.height(316.dp),
            rows = GridCells.Fixed(2),
            horizontalArrangement = spacedBy(16.dp),
            verticalArrangement = spacedBy(16.dp),
        ) {
            items(Categories.values()) { category ->
                CategoryCard(
                    modifier = Modifier.requiredSize(150.dp),
                    category = category,
                    onClick = { viewModel.onEvent(ChooseCategoryEvent.CategoryChosen(category)) },
                )
            }
        }
    }
}

