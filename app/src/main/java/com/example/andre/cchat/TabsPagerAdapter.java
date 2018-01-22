package com.example.andre.cchat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Wijaya_PC on 21-Jan-18.
 */

class TabsPagerAdapter extends FragmentPagerAdapter
{
    public TabsPagerAdapter(FragmentManager fm)
    {
        super(fm);
    }

    @Override
    // get position of our fragments, isal friends fragment dll
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0 :
                RequestsFragment requestsFragment = new RequestsFragment();
                return requestsFragment;

            case 1 :
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;

            case 2:
                FriendsFragment friendsFragment = new FriendsFragment();
                return friendsFragment;

            default:
                return null;
        }
    }


    @Override
    public int getCount() {
       // karena punya 3 fragments
        return 3;
    }

    // kasi judul tab / page dari fragmentnya
    public CharSequence getPageTitle(int position)
    {
        switch (position)
        {
            case 0:
                return "Requests";

            case 1:
                return "Chats";

            case 2:
                return "Friends";

            default:
                return null;
        }
    }
}
