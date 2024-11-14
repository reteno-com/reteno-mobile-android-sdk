package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reteno.core.domain.model.recommendation.post.RecomEvent
import com.reteno.core.domain.model.recommendation.post.RecomEventType
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentRecommendationsPostBinding
import java.time.ZonedDateTime

class FragmentRecommendationsPost : BaseFragment() {

    private var binding: FragmentRecommendationsPostBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecommendationsPostBinding.inflate(
            layoutInflater, container, false
        )
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnPostImpression.setOnClickListener { post(RecomEventType.IMPRESSIONS) }
        binding!!.btnPostClick.setOnClickListener { post(RecomEventType.CLICKS) }
    }

    private fun post(eventType: RecomEventType) {
        val recomEvent = RecomEvent(
            recomEventType = eventType,
            occurred = ZonedDateTime.now(),
            productId = binding!!.etProductId.getText().toString()
        )
        val eventList: MutableList<RecomEvent> = mutableListOf(recomEvent)
        val recomEvents = RecomEvents(
            recomVariantId = binding!!.etRecomVariantId.getText().toString(),
            recomEvents = eventList
        )
        reteno.recommendation.logRecommendations(recomEvents)
    }
}