/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.databinding.FragmentUnitsBinding
import com.madness.collision.main.MainViewModel
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.util.TaggedFragment
import com.madness.collision.util.X
import com.madness.collision.util.availableWidth
import kotlin.math.roundToInt

internal class UnitsFragment : TaggedFragment() {

    override val category: String = "Units"
    override val id: String = "Units"

    companion object {

        @JvmStatic
        fun newInstance() : UnitsFragment {
            return UnitsFragment()
        }
    }

    private lateinit var mViews: FragmentUnitsBinding
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        mViews = FragmentUnitsBinding.inflate(inflater, container, false)
        return mViews.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context: Context = context ?: return
        val mainViewModel: MainViewModel by activityViewModels()

        mRecyclerView = mViews.unitsRecyclerView

        val unitWidth = X.size(context, 400f, X.DP)
        val spanCount = (availableWidth / unitWidth).roundToInt().run {
            if (this < 2) 1 else this
        }
        val mAdapter = UnitsAdapter(context, mainViewModel)
        mAdapter.spanCount = spanCount
        mRecyclerView.layoutManager = mAdapter.suggestLayoutManager(context)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.setItemViewCacheSize(mAdapter.itemCount)
    }
}
