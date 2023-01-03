package com.reteno.push

object Constants {
    const val META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED = "com.reteno.Receiver.PushReceived"
    const val META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED = "com.reteno.Receiver.NotificationClicked"

    const val KEY_ES_INTERACTION_ID = "es_interaction_id"
    const val KEY_ES_TITLE = "es_title"
    const val KEY_ES_CONTENT = "es_content"
    const val KEY_ES_NOTIFICATION_IMAGE = "es_notification_image"
    const val KEY_ES_LINK_WRAPPED = "es_link"
    const val KEY_ES_LINK_UNWRAPPED = "es_link_raw"
    const val KEY_ES_BADGE_COUNT = "es_badge_count"

    const val KEY_ES_BUTTONS = "es_buttons"
    const val KEY_ES_BUTTON_ACTION_ID = "action_id"
    const val KEY_ES_BUTTON_LABEL = "label"
    const val KEY_ES_BUTTON_LINK_WRAPPED = "link"
    const val KEY_ES_BUTTON_LINK_UNWRAPPED = "link_raw"
    const val KEY_ES_BUTTON_CUSTOM_DATA = "custom_data"

    const val KEY_NOTIFICATION_ID = "es_notification_id"
    const val KEY_ACTION_BUTTON = "es_action_button"
    const val KEY_BTN_ACTION_ID = "es_btn_action_id"
    const val KEY_BTN_ACTION_LABEL = "es_btn_action_label"
    const val KEY_BTN_ACTION_LINK_WRAPPED = "es_btn_action_link_wrapped"
    const val KEY_BTN_ACTION_LINK_UNWRAPPED = "es_btn_action_link_unwrapped"
    const val KEY_BTN_ACTION_CUSTOM_DATA = "es_btn_action_custom_data"
    const val MAX_ACTION_BUTTONS = 3
}