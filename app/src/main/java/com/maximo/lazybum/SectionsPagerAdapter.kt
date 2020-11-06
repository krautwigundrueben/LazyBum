package com.maximo.lazybum

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.maximo.lazybum.Globals.AVREC_TAB_POS
import com.maximo.lazybum.Globals.DEVICES_TAB_POS
import com.maximo.lazybum.Globals.SCENES_TAB_POS
import com.maximo.lazybum.Globals.SHUTTER_TAB_POS
import com.maximo.lazybum.fragments.AvReceiverFragment
import com.maximo.lazybum.fragments.DevicesFragment
import com.maximo.lazybum.fragments.ScenesFragment
import com.maximo.lazybum.fragments.ShutterFragment

private val TAB_TITLES = arrayOf(
    R.string.tab_devices,
    R.string.tab_scenes,
    R.string.tab_av_receiver,
    R.string.tab_shutter
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        when (position) {
            DEVICES_TAB_POS -> return DevicesFragment()
            SCENES_TAB_POS -> return ScenesFragment()
            AVREC_TAB_POS -> return AvReceiverFragment()
            SHUTTER_TAB_POS -> return ShutterFragment()
            else -> return DevicesFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return 4
    }
}