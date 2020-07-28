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

import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference

abstract class UpdatesProvider: Updatable {

    protected var fragmentRef: WeakReference<Fragment>? = null
    protected val mFragment: Fragment?
        get() = fragmentRef?.get()
    protected val updatable: Updatable?
        get() {
            val f = mFragment ?: return null
            return if (f is Updatable) f else null
        }

    open fun getFragment(): Fragment? {
        return null
    }

    override fun hasUpdates(hostFragment: Fragment): Boolean {
        return updatable?.hasUpdates(hostFragment) ?: false
    }

    override fun updateState() {
        updatable?.updateState()
    }

}
