package io.github.sspanak.tt9.preferences;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import io.github.sspanak.tt9.Logger;
import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.db.DictionaryLoader;
import io.github.sspanak.tt9.db.LegacyDb;
import io.github.sspanak.tt9.db.WordStoreAsync;
import io.github.sspanak.tt9.ime.helpers.InputModeValidator;
import io.github.sspanak.tt9.ime.helpers.SystemSettings;
import io.github.sspanak.tt9.preferences.helpers.Hotkeys;
import io.github.sspanak.tt9.preferences.screens.AppearanceScreen;
import io.github.sspanak.tt9.preferences.screens.BaseScreenFragment;
import io.github.sspanak.tt9.preferences.screens.DebugScreen;
import io.github.sspanak.tt9.preferences.screens.DictionariesScreen;
import io.github.sspanak.tt9.preferences.screens.HotkeysScreen;
import io.github.sspanak.tt9.preferences.screens.KeyPadScreen;
import io.github.sspanak.tt9.preferences.screens.MainSettingsScreen;
import io.github.sspanak.tt9.preferences.screens.SetupScreen;
import io.github.sspanak.tt9.preferences.screens.UsageStatsScreen;
import io.github.sspanak.tt9.ui.DictionaryLoadingBar;

public class PreferencesActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
	public SettingsStore settings;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = new SettingsStore(this);
		applyTheme();
		Logger.setLevel(settings.getLogLevel());

		try (LegacyDb db = new LegacyDb(this)) { db.clear(); }
		WordStoreAsync.init(this);

		InputModeValidator.validateEnabledLanguages(this, settings.getEnabledLanguageIds());
		validateFunctionKeys();

		super.onCreate(savedInstanceState);

		// changing the theme causes onCreate(), which displays the MainSettingsScreen,
		// but leaves the old "back" history, which is no longer valid,
		// so we must reset it
		getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

		buildLayout();
	}


	@Override
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
		BaseScreenFragment fragment = getScreen((getScreenName(pref)));
		fragment.setArguments(pref.getExtras());
		displayScreen(fragment, true);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!SystemSettings.isTT9Enabled(this)) {
			return;
		}

		Intent intent = getIntent();
		String screenName = intent != null ? intent.getStringExtra("screen") : null;
		screenName = screenName != null ? screenName : "";

		BaseScreenFragment screen = getScreen(screenName);

		if (screen.getName().equals(screenName)) {
			displayScreen(screen, false);
		}
	}

	/**
	 * getScreenName
	 * Determines the name of the screen for the given preference, as defined in the preference's "fragment" attribute.
	 * Expected format: "current.package.name.screens.SomeNameScreen"
	 */
	private String getScreenName(@NonNull Preference pref) {
		String screenClassName = pref.getFragment();
		return screenClassName != null ? screenClassName.replaceFirst("^.+?([^.]+)Screen$", "$1") : "";
	}


	/**
	 * getScreen
	 * Finds a screen fragment by name. If there is no fragment with such name, the main screen
	 * fragment will be returned.
	 */
	private BaseScreenFragment getScreen(@Nullable String name) {
		if (name == null) {
			return new MainSettingsScreen(this);
		}

		switch (name) {
			case AppearanceScreen.NAME:
				return new AppearanceScreen(this);
			case DebugScreen.NAME:
				return new DebugScreen(this);
			case DictionariesScreen.NAME:
				return new DictionariesScreen(this);
			case HotkeysScreen.NAME:
				return new HotkeysScreen(this);
			case KeyPadScreen.NAME:
				return new KeyPadScreen(this);
			case SetupScreen.NAME:
				return new SetupScreen(this);
			case UsageStatsScreen.NAME:
				return new UsageStatsScreen(this);
			default:
				return new MainSettingsScreen(this);
		}
	}


	/**
	 * displayScreen
	 * Replaces the currently displayed screen fragment with a new one.
	 */
	private void displayScreen(BaseScreenFragment screen, boolean addToBackStack) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.preferences_container, screen);
		if (addToBackStack) {
			transaction.addToBackStack(screen.getClass().getSimpleName());
		}

		transaction.commit();
	}


	private void buildLayout() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true); // hide the "back" button, if visible
		}

		setContentView(R.layout.preferences_container);
		displayScreen(getScreen("default"), false);
	}


	public void setScreenTitle(int title) {
		// set the title
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(title);
		}
	}


	private void applyTheme() {
		AppCompatDelegate.setDefaultNightMode(settings.getTheme());
	}


	private void validateFunctionKeys() {
		if (settings.areHotkeysInitialized()) {
			Hotkeys.setDefault(settings);
		}
	}


	public DictionaryLoadingBar getDictionaryProgressBar() {
		return DictionaryLoadingBar.getInstance(this);
	}


	public DictionaryLoader getDictionaryLoader() {
		return DictionaryLoader.getInstance(this);
	}
}
