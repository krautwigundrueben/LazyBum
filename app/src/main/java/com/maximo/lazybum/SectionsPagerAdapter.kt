package com.maximo.lazybum

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.maximo.lazybum.Globals.AVREC_TAB_POS
import com.maximo.lazybum.Globals.DEVICES_TAB_POS
import com.maximo.lazybum.Globals.SCENES_TAB_POS
import com.maximo.lazybum.Globals.SHUTTER_TAB_POS
import com.maximo.lazybum.Globals.VACUUM_TAB_POS
import com.maximo.lazybum.fragments.*

private val TAB_TITLES = arrayOf(
    R.string.tab_devices,
    R.string.tab_scenes,
    R.string.tab_av_receiver,
    R.string.tab_shutter,
    R.string.tab_vacuum
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
@Suppress("DEPRECATION")
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        return when (position) {
            DEVICES_TAB_POS -> DevicesFragment()
            SCENES_TAB_POS -> ScenesFragment()
            AVREC_TAB_POS -> AvReceiverFragment()
            SHUTTER_TAB_POS -> ShutterFragment()
            VACUUM_TAB_POS -> VacuumFragment()
            else -> DevicesFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return 5
    }
}