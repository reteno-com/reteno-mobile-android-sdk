package com.reteno.sample.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.domain.model.recommendation.get.RecomRequest;
import com.reteno.core.data.remote.model.recommendation.get.Recoms;
import com.reteno.core.recommendation.GetRecommendationResponseCallback;
import com.reteno.core.util.Logger;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentRecommendationsGetBinding;
import com.reteno.sample.databinding.ItemRecommendationsGetBinding;
import com.reteno.sample.fragments.database.ViewHolderListener;
import com.reteno.sample.model.RecommendationResponseFull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FragmentRecommendationsGet extends BaseFragment {

    public static final String TAG = FragmentRecommendationsGet.class.getSimpleName();

    private FragmentRecommendationsGetBinding binding;
    private RecommendationAdapter adapter;

    public FragmentRecommendationsGet() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecommendationsGetBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initAdapter();
        initRecycler();
        initListeners();
    }

    private void initAdapter() {
        adapter = new RecommendationAdapter(() -> TransitionManager.beginDelayedTransition(binding.rvList, new AutoTransition()));
    }

    private void initRecycler() {
        binding.rvList.setAdapter(adapter);
    }

    private void initListeners() {
        binding.btnFetch.setOnClickListener(v -> {
            String variantId = binding.etRecomVariantId.getText().toString();

            String productsString = binding.etProducts.getText().toString();
            List<String> products = null;
            if (!TextUtils.isEmpty(productsString)) {
                products = Arrays.asList(productsString.split(","));
            }

            String category = binding.etCategory.getText().toString();
            if (TextUtils.isEmpty(category)) category = null;

            String fieldsString = binding.etFields.getText().toString();
            List<String> fields = null;
            if (fieldsString.equals("null")) {
                fields = null;
            } else if (TextUtils.isEmpty(fieldsString)) {
                fields = new ArrayList<>();
            } else {
                fields = Arrays.asList(fieldsString.split(","));
            }

            RecomRequest request = new RecomRequest(products, category, fields);
            getReteno().getRecommendation().fetchRecommendation(variantId, request, RecommendationResponseFull.class, new GetRecommendationResponseCallback<RecommendationResponseFull>() {
                @Override
                public void onSuccess(@NonNull Recoms<RecommendationResponseFull> response) {
                    adapter.setItems(response.getRecoms());
                }

                @Override
                public void onSuccessFallbackToJson(@NonNull String response) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccessFallbackToJson(): ", "response = [" , response , "]");
                    /*@formatter:on*/
                    Toast.makeText(getContext(), "Failed to parse Json fallback to String response. See Logcat", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@Nullable Integer statusCode, @Nullable String response, @Nullable Throwable throwable) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]");
                    /*@formatter:on*/
                    Toast.makeText(getContext(), "Error occurred. See Logcat", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    //==============================================================================================

    private class RecommendationAdapter extends RecyclerView.Adapter<RecommendationViewHolder> {

        private List<RecommendationResponseFull> items = new ArrayList<>();
        private final ViewHolderListener onExpandCollapseClickListener;

        private RecommendationAdapter(ViewHolderListener listener) {
            this.onExpandCollapseClickListener = listener;
        }

        void setItems(List<RecommendationResponseFull> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRecommendationsGetBinding binding = ItemRecommendationsGetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new RecommendationViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
            RecommendationResponseFull item = items.get(position);
            initListeners(holder.bindingHolder);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void initListeners(ItemRecommendationsGetBinding binding) {
            binding.ivExpand.setOnClickListener(v -> {
                if (binding.llContent.getVisibility() == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.GONE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more);
                } else {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.VISIBLE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less);
                }
            });
        }
    }

    //==============================================================================================

    private class RecommendationViewHolder extends RecyclerView.ViewHolder {
        final ItemRecommendationsGetBinding bindingHolder;

        private RecommendationViewHolder(ItemRecommendationsGetBinding binding) {
            super(binding.getRoot());
            this.bindingHolder = binding;
        }

        private void bind(RecommendationResponseFull model) {
            bindingHolder.tvProductId.setText(model.getProductId());
            if (model.getCategory() != null) {
                bindingHolder.tvCategory.setVisibility(View.VISIBLE);
                bindingHolder.tvCategory.setText(String.join(", ", model.getCategory()));
            } else {
                bindingHolder.tvCategory.setVisibility(View.GONE);
            }
            if (model.getCategoryAncestor() != null) {
                bindingHolder.tvCategoryAncestor.setVisibility(View.VISIBLE);
                bindingHolder.tvCategoryAncestor.setText(String.join(", ", model.getCategoryAncestor()));
            } else {
                bindingHolder.tvCategoryAncestor.setVisibility(View.GONE);
            }
            if (model.getCategoryLayout() != null) {
                bindingHolder.tvCategoryLayout.setVisibility(View.VISIBLE);
                bindingHolder.tvCategoryLayout.setText(String.join(", ", model.getCategoryLayout()));
            } else {
                bindingHolder.tvCategoryLayout.setVisibility(View.GONE);
            }
            if (model.getCategoryParent() != null) {
                bindingHolder.tvCategoryParent.setVisibility(View.VISIBLE);
                bindingHolder.tvCategoryParent.setText(String.join(", ", model.getCategoryParent()));
            } else {
                bindingHolder.tvCategoryParent.setVisibility(View.GONE);
            }
            bindingHolder.tvDateCreatedAs.setTextOrHide(model.getDate_created_as());
            bindingHolder.tvDateCreatedEs.setTextOrHide(model.getDate_created_es());
            bindingHolder.tvDateModifiedAs.setTextOrHide(model.getDate_modified_as());
            bindingHolder.tvDescr.setTextOrHide(model.getDescr());
            bindingHolder.tvImageUrl.setTextOrHide(model.getImageUrl());
            bindingHolder.tvItemGroup.setTextOrHide(model.getItem_group());
            bindingHolder.tvName.setTextOrHide(model.getName());
            bindingHolder.tvNameKeyword.setTextOrHide(model.getName_keyword());
            bindingHolder.tvPrice.setTextOrHide(String.valueOf(model.getPrice()));
            bindingHolder.tvProductId2.setTextOrHide(model.getProduct_id());
            bindingHolder.tvTagsAllCategoryNames.setTextOrHide(model.getTags_all_category_names());
            bindingHolder.tvTagsBestseller.setTextOrHide(model.getTags_bestseller());
            bindingHolder.tvTagsCashback.setTextOrHide(model.getTags_cashback());
            bindingHolder.tvTagsCategoryBestseller.setTextOrHide(model.getTags_category_bestseller());
            bindingHolder.tvTagsCredit.setTextOrHide(model.getTags_credit());
            bindingHolder.tvTagsDelivery.setTextOrHide(model.getTags_delivery());
            bindingHolder.tvTagsDescriptionPriceRange.setTextOrHide(model.getTags_description_price_range());
            bindingHolder.tvTagsDiscount.setTextOrHide(model.getTags_discount());
            bindingHolder.tvTagsHasPurchases21Days.setTextOrHide(model.getTags_has_purchases_21_days());
            bindingHolder.tvTagsIsBestseller.setTextOrHide(model.getTags_is_bestseller());
            bindingHolder.tvTagsIsBestsellerByCategories.setTextOrHide(model.getTags_is_bestseller_by_categories());
            bindingHolder.tvTagsItemGroupId.setTextOrHide(model.getTags_item_group_id());
            bindingHolder.tvTagsNumPurchases21Days.setTextOrHide(model.getTags_num_purchases_21_days());
            bindingHolder.tvTagsOldPrice.setTextOrHide(model.getTags_old_price());
            bindingHolder.tvTagsOldPrice2.setTextOrHide(model.getTags_oldprice());
            bindingHolder.tvTagsPriceRange.setTextOrHide(model.getTags_price_range());
            bindingHolder.tvTagsRating.setTextOrHide(model.getTags_rating());
            bindingHolder.tvTagsSale.setTextOrHide(model.getTags_sale());
            bindingHolder.tvUrl.setTextOrHide(model.getUrl());
        }
    }
}