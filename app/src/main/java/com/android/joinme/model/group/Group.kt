package com.android.joinme.model.group

/**
 * Represents a group that can organize multiple events and manage members.
 *
 * @property id The unique identifier for the group.
 * @property name The name of the group.
 * @property description A description of the group's purpose or activities.
 * @property ownerId The user ID of the group owner who has permissions to modify settings, add
 *   events, and manage members.
 * @property memberIds List of user IDs belonging to this group. Size determines membersCount.
 * @property eventIds List of event IDs that belong to this group.
 * @property membersCount The total number of members in the group (derived from memberIds size).
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val eventIds: List<String> = emptyList(),
    val membersCount: Int = memberIds.size
)
