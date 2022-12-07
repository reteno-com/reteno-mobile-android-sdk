package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.domain.model.recommendation.post.RecomEvent;
import com.reteno.core.domain.model.recommendation.post.RecomEventType;
import com.reteno.core.domain.model.recommendation.post.RecomEvents;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentRecommendationsPostBinding;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class FragmentRecommendationsPost extends BaseFragment {

    private FragmentRecommendationsPostBinding binding;

    public FragmentRecommendationsPost() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecommendationsPostBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnPostImpression.setOnClickListener(v -> {
            post(RecomEventType.IMPRESSIONS);
        });
        binding.btnPostClick.setOnClickListener(v -> {
            post(RecomEventType.CLICKS);
        });
    }

    private void post(RecomEventType eventType) {
        RecomEvent recomEvent = new RecomEvent(eventType, ZonedDateTime.now(), binding.etProductId.getText().toString());
        List<RecomEvent> eventList = new ArrayList<>();
        eventList.add(recomEvent);

        RecomEvents recomEvents = new RecomEvents(binding.etRecomVariantId.getText().toString(), eventList);
        getReteno().getRecommendation().logRecommendations(recomEvents);
    }
}