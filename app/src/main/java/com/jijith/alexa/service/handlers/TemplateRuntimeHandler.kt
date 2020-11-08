package com.jijith.alexa.service.handlers

import android.util.Log
import androidx.annotation.Nullable
import com.amazon.aace.alexa.TemplateRuntime
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class TemplateRuntimeHandler(private var playbackController: PlaybackControllerHandler) : TemplateRuntime() {

    private var mCurrentAudioItemId: String? = null

    override fun renderTemplate(payload: String?) {
        try {
            // Log payload
            val template = JSONObject(payload)
//            mLogger!!.postJSONTemplate(TemplateRuntimeHandler.sTag, template.toString(4))
            Timber.i( payload)
            // Log card
            val type = template.getString("type")
            /*when (type) {
                "BodyTemplate1" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.BODY_TEMPLATE1
                )
                "BodyTemplate2" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.BODY_TEMPLATE2
                )
                "ListTemplate1" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.LIST_TEMPLATE1
                )
                "WeatherTemplate" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.WEATHER_TEMPLATE
                )
                "LocalSearchListTemplate1" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.LOCAL_SEARCH_LIST_TEMPLATE1
                )
                "LocalSearchListTemplate2" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.LOCAL_SEARCH_LIST_TEMPLATE2
                )
                "TrafficDetailsTemplate" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.TRAFFIC_DETAILS_TEMPLATE
                )
                "LocalSearchDetailTemplate1" -> mLogger!!.postDisplayCard(
                    template,
                    LogRecyclerViewAdapter.LOCAL_SEARCH_DETAIL_TEMPLATE1
                )
                else -> mLogger!!.postError(TemplateRuntimeHandler.sTag, "Unknown Template sent")
            }*/
        } catch (e: JSONException) {
            Timber.e(e.message)
        }
    }

    override fun renderPlayerInfo(payload: String?) {
        Timber.d(payload)
       /* try {
            val playerInfo = JSONObject(payload)
            val audioItemId = playerInfo.getString("audioItemId")
            val content = playerInfo.getJSONObject("content")
            val audioProvider = content.getJSONObject("provider")
            val providerName = audioProvider.getString("name")

            // Update playback controller buttons and player info labels
            if (mPlaybackController != null) {
                //reset visual state
                mPlaybackController.hidePlayerInfoControls()
                if (playerInfo.has("controls")) {
                    val controls = playerInfo.getJSONArray("controls")
                    for (j in 0 until controls.length()) {
                        val control = controls.getJSONObject(j)
                        if (control.getString("type") == "BUTTON") {
                            val enabled = control.getBoolean("enabled")
                            val name = control.getString("name")
                            mPlaybackController.updateControlButton(name, enabled)
                        } else if (control.getString("type") == "TOGGLE") {
                            val selected = control.getBoolean("selected")
                            val enabled = control.getBoolean("enabled")
                            val name = control.getString("name")
                            mPlaybackController.updateControlToggle(name, enabled, selected)
                        }
                    }
                }
                val title =
                    if (content.has("title")) content.getString("title") else ""
                val artist =
                    if (content.has("titleSubtext1")) content.getString("titleSubtext1") else ""
                val provider = content.getJSONObject("provider")
                val name =
                    if (provider.has("name")) provider.getString("name") else ""
                mPlaybackController.setPlayerInfo(title, artist, name)
            }

            // Log only if audio item has changed
            if (audioItemId != mCurrentAudioItemId) {
                mCurrentAudioItemId = audioItemId

                // Log payload
                mLogger!!.postJSONTemplate(TemplateRuntimeHandler.sTag, playerInfo.toString(4))

                // Log card
                mLogger!!.postDisplayCard(content, LogRecyclerViewAdapter.RENDER_PLAYER_INFO)
            } else {
                mLogger!!.postJSONTemplate(TemplateRuntimeHandler.sTag, playerInfo.toString(4))
            }
        } catch (e: JSONException) {
            mLogger!!.postError(TemplateRuntimeHandler.sTag, e.message)
        }*/
    }

    override fun clearTemplate() {
        // Handle dismissing display card here
        Timber.d("handle clearTemplate()")
    }

    override fun clearPlayerInfo() {
        Timber.d("handle clearPlayerInfo()")
       /* if (mPlaybackController != null && !mPlaybackController.getProvider()
                .equals(MACCPlayer.SPOTIFY_PROVIDER_NAME)
        ) {
            mPlaybackController.setPlayerInfo("", "", "")
            mPlaybackController.hidePlayerInfoControls()
        }*/
    }
}