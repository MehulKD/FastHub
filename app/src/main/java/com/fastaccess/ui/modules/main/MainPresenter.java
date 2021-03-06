package com.fastaccess.ui.modules.main;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import com.fastaccess.R;
import com.fastaccess.helper.Logger;
import com.fastaccess.helper.PrefGetter;
import com.fastaccess.helper.RxHelper;
import com.fastaccess.provider.rest.RestProvider;
import com.fastaccess.ui.base.mvp.presenter.BasePresenter;
import com.fastaccess.ui.modules.feeds.FeedsFragment;
import com.fastaccess.ui.modules.main.issues.pager.MyIssuesPagerFragment;
import com.fastaccess.ui.modules.main.pullrequests.pager.MyPullsPagerFragment;

import java.util.Observable;

import static com.fastaccess.helper.ActivityHelper.getVisibleFragment;
import static com.fastaccess.helper.AppHelper.getFragmentByTag;

/**
 * Created by Kosh on 09 Nov 2016, 7:53 PM
 */

public class MainPresenter extends BasePresenter<MainMvp.View> implements MainMvp.Presenter {

    MainPresenter() {
        setEnterprise(PrefGetter.isEnterprise());
        manageDisposable(RxHelper.getObserver(RestProvider.getUserService(isEnterprise()).getUser())
                .flatMap(login -> {
                    login.setIsLoggedIn(true);
                    login.setEnterpriseUrl(isEnterprise() ? PrefGetter.getEnterpriseUrl() : null);
                    login.setToken(isEnterprise() ? PrefGetter.getEnterpriseToken() : PrefGetter.getToken());
                    login.setOtpCode(isEnterprise() ? PrefGetter.getEnterpriseOtpCode() : PrefGetter.getOtpCode());
                    login.setIsEnterprise(isEnterprise());
                    return login.update(login);
                })
                .subscribe(login -> {
                    Logger.e(login.getToken());
                    if (login != null) {
                        sendToView(MainMvp.View::onUpdateDrawerMenuHeader);
                    }
                }, Throwable::printStackTrace/*fail silently*/));
    }

    @Override public boolean canBackPress(@NonNull DrawerLayout drawerLayout) {
        return !drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    @SuppressWarnings("ConstantConditions")
    @Override public void onModuleChanged(@NonNull FragmentManager fragmentManager, @MainMvp.NavigationType int type) {
        Fragment currentVisible = getVisibleFragment(fragmentManager);
        FeedsFragment homeView = (FeedsFragment) getFragmentByTag(fragmentManager, FeedsFragment.TAG);
        MyPullsPagerFragment pullRequestView = (MyPullsPagerFragment) getFragmentByTag(fragmentManager, MyPullsPagerFragment.TAG);
        MyIssuesPagerFragment issuesView = (MyIssuesPagerFragment) getFragmentByTag(fragmentManager, MyIssuesPagerFragment.TAG);
        switch (type) {
            case MainMvp.PROFILE:
                sendToView(MainMvp.View::onOpenProfile);
                break;
            case MainMvp.FEEDS:
                if (homeView == null) {
                    onAddAndHide(fragmentManager, FeedsFragment.newInstance(isEnterprise()), currentVisible);
                } else {
                    onShowHideFragment(fragmentManager, homeView, currentVisible);
                }
                break;
            case MainMvp.PULL_REQUESTS:
                if (pullRequestView == null) {
                    onAddAndHide(fragmentManager, MyPullsPagerFragment.newInstance(isEnterprise()), currentVisible);
                } else {
                    onShowHideFragment(fragmentManager, pullRequestView, currentVisible);
                }
                break;
            case MainMvp.ISSUES:
                if (issuesView == null) {
                    onAddAndHide(fragmentManager, MyIssuesPagerFragment.newInstance(isEnterprise()), currentVisible);
                } else {
                    onShowHideFragment(fragmentManager, issuesView, currentVisible);
                }
                break;
        }
    }

    @Override public void onShowHideFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment toShow, @NonNull Fragment toHide) {
        toHide.onHiddenChanged(true);
        fragmentManager
                .beginTransaction()
                .hide(toHide)
                .show(toShow)
                .commit();
        toShow.onHiddenChanged(false);
    }

    @Override public void onAddAndHide(@NonNull FragmentManager fragmentManager, @NonNull Fragment toAdd, @NonNull Fragment toHide) {
        toHide.onHiddenChanged(true);
        fragmentManager
                .beginTransaction()
                .hide(toHide)
                .add(R.id.container, toAdd, toAdd.getClass().getSimpleName())
                .commit();
        toAdd.onHiddenChanged(false);
    }

    @Override public void onMenuItemSelect(@IdRes int id, int position, boolean fromUser) {
        if (getView() != null) {
            getView().onNavigationChanged(position);
        }
    }

    @Override public void onMenuItemReselect(@IdRes int id, int position, boolean fromUser) {}
}
