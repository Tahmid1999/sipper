package io.github.tahmid1999.sipper.app.ui

/**
 * The committed column sets, CONTRACT.md §4 (AUDIT) and §5 (APPS). Widths are the contract's dp
 * figures, which equal `chars × 7.2dp + 12dp` at the measured advance of the bundled face; the
 * ColumnsTest asserts both the sum totals and the per-column derivation.
 */

/** AUDIT: six columns, one frozen. CONTRACT.md §4. */
val AuditColumns: List<TableColumn> = listOf(
    TableColumn(id = "key", title = "key", chars = 22, align = ColumnAlign.Left),
    TableColumn(id = "verdict", title = "verdict", chars = 0, align = ColumnAlign.Left), // fixed 100dp chip
    TableColumn(id = "value", title = "value", chars = 8, align = ColumnAlign.Right),
    TableColumn(id = "eq", title = "=", chars = 2, align = ColumnAlign.Center),
    TableColumn(id = "from", title = "from", chars = 12, align = ColumnAlign.Left),
    TableColumn(id = "effect", title = "effect", chars = 28, align = ColumnAlign.Left),
)

/** APPS: fourteen columns, one frozen. CONTRACT.md §5. */
val AppsColumns: List<TableColumn> = listOf(
    TableColumn(id = "package", title = "package", chars = 18, align = ColumnAlign.Left),
    TableColumn(id = "fgfw", title = "fg fw", chars = 6, align = ColumnAlign.Right),
    TableColumn(id = "fgevt", title = "fg evt", chars = 6, align = ColumnAlign.Right),
    TableColumn(id = "delta", title = "Δ fg", chars = 7, align = ColumnAlign.Right),
    TableColumn(id = "label", title = "label", chars = 18, align = ColumnAlign.Left),
    TableColumn(id = "visible", title = "visible", chars = 6, align = ColumnAlign.Right),
    TableColumn(id = "fgs", title = "fgs", chars = 6, align = ColumnAlign.Right),
    TableColumn(id = "tail", title = "tail", chars = 5, align = ColumnAlign.Left),
    TableColumn(id = "rxfg", title = "rx fg", chars = 7, align = ColumnAlign.Right),
    TableColumn(id = "txfg", title = "tx fg", chars = 7, align = ColumnAlign.Right),
    TableColumn(id = "rxbg", title = "rx bg", chars = 7, align = ColumnAlign.Right),
    TableColumn(id = "txbg", title = "tx bg", chars = 7, align = ColumnAlign.Right),
    TableColumn(id = "bucket", title = "bucket", chars = 14, align = ColumnAlign.Left),
    TableColumn(id = "mah", title = "mAh", chars = 7, align = ColumnAlign.Right),
)
