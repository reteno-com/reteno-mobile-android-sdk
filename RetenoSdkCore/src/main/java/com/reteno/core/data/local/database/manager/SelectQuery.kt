package com.reteno.core.data.local.database.manager

internal fun selectColumns(columns: List<Pair<String, String>>): String =
    columns.joinToString(separator = ", ", prefix = "SELECT ") { (table, column) -> "$table.$column AS $column" }

internal fun limitClause(limit: Int?): String = if (limit == null) "" else " LIMIT $limit"
