package unipg.dmi.robook;

import unipg.dmi.robook.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * 
 * @author Abdul Rasheed Tutakhail
 * 
 */
public class prefs extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}

}
