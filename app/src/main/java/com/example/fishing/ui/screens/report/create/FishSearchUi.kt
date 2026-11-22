package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishSuggestion
import com.example.fishing.model.FishSuggestionSource
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.SearchInputField
import com.example.fishing.ui.theme.FishingTheme

/**
 * Collapsed search bar shown in the app bar of the catch screen. It is a
 * button, not an editable field: tapping it opens [FishSearchView].
 */
@Composable
fun FishSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .testTag("fish_search_bar"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.fish_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Full-screen catch search. Stateless: the caller owns the query, the catalog
 * suggestions and the "already added" predicate, so the same composable can be
 * driven by a ViewModel or by a preview/test fake.
 */
@Composable
fun FishSearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    recent: List<FishSuggestion>,
    popular: List<FishSuggestion>,
    results: List<FishSpecies>,
    isAlreadyAdded: (name: String, speciesId: String?) -> Boolean,
    onBack: () -> Unit,
    onSelect: (name: String, speciesId: String?) -> Unit,
    onAddCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("fish_search_view"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SearchInputField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.fish_search_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fish_search_input"),
                    minHeight = 56.dp,
                    focusRequester = focusRequester,
                    leadingContent = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = onAddCustom,
                            enabled = query.isNotBlank(),
                            modifier = Modifier.testTag("fish_search_add")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add),
                                tint = if (query.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
            }

            val history = recent.ifEmpty { popular }
            val historyTitle = if (recent.isEmpty()) {
                R.string.fish_popular_species
            } else {
                R.string.fish_recently_added
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .testTag("fish_search_content"),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (history.isNotEmpty()) {
                    item {
                        FishSectionTitle(stringResource(historyTitle))
                    }
                    items(
                        items = history,
                        key = { "history:${it.speciesId ?: it.name}" }
                    ) { suggestion ->
                        val added = isAlreadyAdded(suggestion.name, suggestion.speciesId)
                        val select: () -> Unit = {
                            onSelect(suggestion.name, suggestion.speciesId)
                        }
                        FishingListItem(
                            title = suggestion.name,
                            supportingText = if (added) {
                                stringResource(R.string.already_in_list)
                            } else {
                                null
                            },
                            leadingIcon = if (suggestion.source == FishSuggestionSource.HISTORY) {
                                Icons.Default.History
                            } else {
                                Icons.Default.Search
                            },
                            isError = added,
                            onRowClick = if (added) null else select,
                            modifier = Modifier.testTag("fish_suggestion_${suggestion.name}")
                        )
                    }
                }

                if (query.isNotBlank() && results.isNotEmpty()) {
                    item {
                        FishSectionTitle(stringResource(R.string.fish_search_results))
                    }
                    items(
                        items = results,
                        key = { "result:${it.id}" }
                    ) { species ->
                        val added = isAlreadyAdded(species.canonicalName, species.id)
                        val select: () -> Unit = {
                            onSelect(species.canonicalName, species.id)
                        }
                        FishingListItem(
                            title = species.canonicalName,
                            fontWeight = FontWeight.Medium,
                            supportingText = if (added) {
                                stringResource(R.string.already_in_list)
                            } else {
                                null
                            },
                            leadingIcon = Icons.Default.Search,
                            isError = added,
                            onRowClick = if (added) null else select,
                            modifier = Modifier.testTag("fish_suggestion_${species.canonicalName}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 820)
@Composable
private fun FishSearchViewPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        FishSearchView(
            query = "щу",
            onQueryChange = {},
            recent = listOf(
                FishSuggestion("Карась", "carassius_gibelio", FishSuggestionSource.HISTORY),
                FishSuggestion("Плотва", "rutilus_rutilus", FishSuggestionSource.HISTORY),
                FishSuggestion("Окунь", "perca_fluviatilis", FishSuggestionSource.HISTORY)
            ),
            popular = emptyList(),
            results = listOf(
                FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3),
                FishSpecies("silurus_glanis", "Сом", emptyList(), isPopular = true, sortOrder = 9)
            ),
            isAlreadyAdded = { name, _ -> name == "Карась" },
            onBack = {},
            onSelect = { _, _ -> },
            onAddCustom = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 120)
@Composable
private fun FishSearchBarPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            FishSearchBar(onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
