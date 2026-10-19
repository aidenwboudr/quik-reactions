/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.compat

import android.content.Context
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import dev.octoshrimpy.quik.manager.PermissionManager
import javax.inject.Inject

class SubscriptionManagerCompat @Inject constructor(context: Context, private val permissions: PermissionManager) {

    private val subscriptionManager: SubscriptionManager?
        get() = field?.takeIf { permissions.hasPhone() }

    val activeSubscriptionInfoList: List<SubscriptionInfoCompat>
        get() {
            return if (permissions.hasPhone()) {
                try {
                    subscriptionManager?.activeSubscriptionInfoList?.map {
                        SubscriptionInfoCompat(it)
                    } ?: listOf()
                } catch (e: SecurityException) {
                    // Handle exception if permission is not granted
                    listOf()
                }
            } else {
                listOf()
            }
        }

    init {
        subscriptionManager = SubscriptionManager.from(context)
    }

    /**
     * The SIM to send from, or null when there's only one. The phone's default SMS SIM wins, so
     * replies don't follow whichever line the last message used. With "Ask every time" there's
     * no default, so it falls back to the SIM of the conversation's latest message.
     */
    fun sendingSubscription(subs: List<SubscriptionInfoCompat>, latestSubId: Int): SubscriptionInfoCompat? {
        if (subs.size <= 1) return null

        val defaultSubId = SmsManager.getDefaultSmsSubscriptionId()
        return subs.firstOrNull { it.subscriptionId == defaultSubId }
            ?: subs.firstOrNull { it.subscriptionId == latestSubId }
            ?: subs[0]
    }

    fun addOnSubscriptionsChangedListener(listener: OnSubscriptionsChangedListener) {
        subscriptionManager?.addOnSubscriptionsChangedListener(listener.listener)
    }

    fun removeOnSubscriptionsChangedListener(listener: OnSubscriptionsChangedListener) {
        subscriptionManager?.removeOnSubscriptionsChangedListener(listener.listener)
    }

    abstract class OnSubscriptionsChangedListener {

        val listener: SubscriptionManager.OnSubscriptionsChangedListener =
            object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    this@OnSubscriptionsChangedListener.onSubscriptionsChanged()
                }
            }

        abstract fun onSubscriptionsChanged()

    }

}