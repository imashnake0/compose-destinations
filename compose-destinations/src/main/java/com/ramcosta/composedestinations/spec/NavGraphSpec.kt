package com.ramcosta.composedestinations.spec

/**
 * Defines a navigation graph.
 */
interface NavGraphSpec: Routed {

    /**
     * Route for this navigation graph.
     * It can be used to navigate to it.
     */
    override val route: String

    /**
     * Start destination of this navigation graph.
     */
    val startDestination: DestinationSpec

    /**
     * All destinations which belong to this navigation graph
     * by their route
     */
    val destinations: Map<String, DestinationSpec>

    /**
     * Nested navigation graphs of this navigation graph.
     */
    val nestedNavGraphs: List<NavGraphSpec> get() = emptyList()
}