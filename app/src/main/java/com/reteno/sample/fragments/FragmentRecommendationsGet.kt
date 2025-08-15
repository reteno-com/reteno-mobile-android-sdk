package com.reteno.sample.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.util.Logger.i
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentRecommendationsGetBinding
import com.reteno.sample.databinding.ItemRecommendationsGetBinding
import com.reteno.sample.fragments.database.ViewHolderListener
import com.reteno.sample.model.RecommendationResponseFull

class FragmentRecommendationsGet : BaseFragment() {
    private var binding: FragmentRecommendationsGetBinding? = null
    private var adapter: RecommendationAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecommendationsGetBinding.inflate(getLayoutInflater(), container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        initRecycler()
        initListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.rvList?.adapter = null
        binding = null
    }

    private fun initAdapter() {
        adapter = RecommendationAdapter {
            TransitionManager.beginDelayedTransition(
                binding!!.rvList, AutoTransition()
            )
        }
    }

    private fun initRecycler() {
        binding!!.rvList.adapter = adapter
    }

    private fun initListeners() {
        binding!!.btnFetch.setOnClickListener {
            val (variantId, request) = buildRecomRequest()
            reteno.recommendation.fetchRecommendation(
                variantId,
                request,
                RecommendationResponseFull::class.java,
                object : GetRecommendationResponseCallback<RecommendationResponseFull> {
                    override fun onSuccess(response: Recoms<RecommendationResponseFull>) {
                        adapter!!.setItems(response.recoms)
                        binding?.rvList?.isVisible = true
                        binding?.jsonSection?.isVisible = false
                    }

                    override fun onSuccessFallbackToJson(response: String) {
                        /*@formatter:off*/
            i(TAG, "onSuccessFallbackToJson(): ", "response = [", response, "]")
            /*@formatter:on*/Toast.makeText(
                            context,
                            "Failed to parse Json fallback to String response. See Logcat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(
                        statusCode: Int?,
                        response: String?,
                        throwable: Throwable?
                    ) {
                        /*@formatter:off*/
            i(TAG, "onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
            /*@formatter:on*/Toast.makeText(
                            context,
                            "Error occurred. See Logcat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        binding!!.btnFetchJson.setOnClickListener {
            val (variantId, request) = buildRecomRequest()
            reteno.recommendation.fetchRecommendationJson(
                variantId,
                request,
                object :GetRecommendationResponseJsonCallback {
                    override fun onSuccess(response: String) {
                        binding?.tvJson?.text = response
                        binding?.rvList?.isVisible = false
                        binding?.jsonSection?.isVisible = true
                    }

                    override fun onFailure(
                        statusCode: Int?,
                        response: String?,
                        throwable: Throwable?
                    ) {
                        /*@formatter:off*/
                        i(TAG, "onFailure(): ", "statusCode = [", statusCode, "], response = [", response, "], throwable = [", throwable, "]")
                        /*@formatter:on*/Toast.makeText(
                            context,
                            "Error occurred. See Logcat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            )
        }
        binding!!.btnCopyJson.setOnClickListener {
            copyToClipboard(binding?.tvJson?.text?.toString())
        }
    }

    private fun copyToClipboard(text: String?) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Custom Data", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun buildRecomRequest(): Pair<String, RecomRequest> {
        val variantId = binding!!.etRecomVariantId.text.toString()
        val productsString = binding!!.etProducts.text.toString()
        var products: List<String>? = null
        if (productsString.isNotEmpty()) {
            products = productsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        }
        var category: String? = binding!!.etCategory.text.toString()
        if (TextUtils.isEmpty(category)) category = null
        val fieldsString = binding!!.etFields.text.toString()
        val fields: List<String>? = if (fieldsString == "null") {
            null
        } else if (TextUtils.isEmpty(fieldsString)) {
            ArrayList()
        } else {
            fieldsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        }
        val text = binding!!.etFilters.text?.toString().orEmpty()
        val filters = text.split(";").mapNotNull {
            if (it.isNotBlank()) {
                val list = it.split(",")
                val name = list.first()
                RecomFilter(
                    name,
                    list.subList(1, list.size)
                )
            } else null
        }
        val request = RecomRequest(products, category, fields, filters)
        return Pair(variantId, request)
    }

    //==============================================================================================
    private inner class RecommendationAdapter(private val onExpandCollapseClickListener: ViewHolderListener) :
        RecyclerView.Adapter<RecommendationViewHolder>() {
        private var items: List<RecommendationResponseFull> = ArrayList()
        fun setItems(newItems: List<RecommendationResponseFull>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecommendationViewHolder {
            val binding = ItemRecommendationsGetBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return RecommendationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
            val item = items[position]
            initListeners(holder.bindingHolder)
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private fun initListeners(binding: ItemRecommendationsGetBinding) {
            binding.ivExpand.setOnClickListener { v: View? ->
                if (binding.llContent.visibility == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.GONE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more)
                } else {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.VISIBLE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less)
                }
            }
        }
    }

    //==============================================================================================
    private inner class RecommendationViewHolder(val bindingHolder: ItemRecommendationsGetBinding) :
        RecyclerView.ViewHolder(
            bindingHolder.root
        ) {
        fun bind(model: RecommendationResponseFull) {
            bindingHolder.tvProductId.setText(model.productId)
            if (model.category != null) {
                bindingHolder.tvCategory.visibility = View.VISIBLE
                bindingHolder.tvCategory.setText(java.lang.String.join(", ", model.category))
            } else {
                bindingHolder.tvCategory.visibility = View.GONE
            }
            if (model.categoryAncestor != null) {
                bindingHolder.tvCategoryAncestor.visibility = View.VISIBLE
                bindingHolder.tvCategoryAncestor.setText(
                    java.lang.String.join(
                        ", ",
                        model.categoryAncestor
                    )
                )
            } else {
                bindingHolder.tvCategoryAncestor.visibility = View.GONE
            }
            if (model.categoryLayout != null) {
                bindingHolder.tvCategoryLayout.visibility = View.VISIBLE
                bindingHolder.tvCategoryLayout.setText(
                    java.lang.String.join(
                        ", ",
                        model.categoryLayout
                    )
                )
            } else {
                bindingHolder.tvCategoryLayout.visibility = View.GONE
            }
            if (model.categoryParent != null) {
                bindingHolder.tvCategoryParent.visibility = View.VISIBLE
                bindingHolder.tvCategoryParent.setText(
                    java.lang.String.join(
                        ", ",
                        model.categoryParent
                    )
                )
            } else {
                bindingHolder.tvCategoryParent.visibility = View.GONE
            }
            bindingHolder.tvDateCreatedAs.setTextOrHide(model.date_created_as)
            bindingHolder.tvDateCreatedEs.setTextOrHide(model.date_created_es)
            bindingHolder.tvDateModifiedAs.setTextOrHide(model.date_modified_as)
            bindingHolder.tvDescr.setTextOrHide(model.descr)
            bindingHolder.tvImageUrl.setTextOrHide(model.imageUrl)
            bindingHolder.tvItemGroup.setTextOrHide(model.item_group)
            bindingHolder.tvName.setTextOrHide(model.name)
            bindingHolder.tvNameKeyword.setTextOrHide(model.name_keyword)
            bindingHolder.tvPrice.setTextOrHide(model.price.toString())
            bindingHolder.tvProductId2.setTextOrHide(model.product_id)
            bindingHolder.tvTagsAllCategoryNames.setTextOrHide(model.tags_all_category_names)
            bindingHolder.tvTagsBestseller.setTextOrHide(model.tags_bestseller)
            bindingHolder.tvTagsCashback.setTextOrHide(model.tags_cashback)
            bindingHolder.tvTagsCategoryBestseller.setTextOrHide(model.tags_category_bestseller)
            bindingHolder.tvTagsCredit.setTextOrHide(model.tags_credit)
            bindingHolder.tvTagsDelivery.setTextOrHide(model.tags_delivery)
            bindingHolder.tvTagsDescriptionPriceRange.setTextOrHide(model.tags_description_price_range)
            bindingHolder.tvTagsDiscount.setTextOrHide(model.tags_discount)
            bindingHolder.tvTagsHasPurchases21Days.setTextOrHide(model.tags_has_purchases_21_days)
            bindingHolder.tvTagsIsBestseller.setTextOrHide(model.tags_is_bestseller)
            bindingHolder.tvTagsIsBestsellerByCategories.setTextOrHide(model.tags_is_bestseller_by_categories)
            bindingHolder.tvTagsItemGroupId.setTextOrHide(model.tags_item_group_id)
            bindingHolder.tvTagsNumPurchases21Days.setTextOrHide(model.tags_num_purchases_21_days)
            bindingHolder.tvTagsOldPrice.setTextOrHide(model.tags_old_price)
            bindingHolder.tvTagsOldPrice2.setTextOrHide(model.tags_oldprice)
            bindingHolder.tvTagsPriceRange.setTextOrHide(model.tags_price_range)
            bindingHolder.tvTagsRating.setTextOrHide(model.tags_rating)
            bindingHolder.tvTagsSale.setTextOrHide(model.tags_sale)
            bindingHolder.tvUrl.setTextOrHide(model.url)
        }
    }

    companion object {
        val TAG = FragmentRecommendationsGet::class.java.simpleName
    }
}